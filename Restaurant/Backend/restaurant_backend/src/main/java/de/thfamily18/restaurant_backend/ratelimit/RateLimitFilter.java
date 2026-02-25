package de.thfamily18.restaurant_backend.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;
    private final MeterRegistry meterRegistry;

    // Metrics (initialized once)
    private Counter allowedCounter;
    private Counter blockedCounter;
    private Counter errorCounter;
    private Timer filterTimer;

    @PostConstruct
    void initMetrics() {
        this.allowedCounter = meterRegistry.counter("ratelimit.allowed");
        this.blockedCounter = meterRegistry.counter("ratelimit.blocked");
        this.errorCounter = meterRegistry.counter("ratelimit.errors");
        this.filterTimer = meterRegistry.timer("ratelimit.filter.time");

        // Dev-only visibility: how many in-memory buckets currently exist
        Gauge.builder("ratelimit.buckets", buckets, ConcurrentHashMap::size)
                .description("Number of in-memory token buckets (dev only)")
                .register(meterRegistry);
    }


    // In-memory buckets (dev only). Key = user/ip + route-group
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String path = request.getRequestURI();

            // Skip actuator (optional)
            if (path.startsWith("/actuator")) {
                filterChain.doFilter(request, response);
                return;
            }

            String key = resolveKey(request, path);

            Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket.builder()
                    .addLimit(limitFor(path))
                    .build());

            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            // Useful headers for debugging/testing
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

            if (!probe.isConsumed()) {
                blockedCounter.increment();
                long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
                response.setHeader("Retry-After", String.valueOf(waitSeconds));
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("""
                {"status":429,"error":"TOO_MANY_REQUESTS","message":"Rate limit exceeded"}
                """);
                return;
            }
            allowedCounter.increment();
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            throw ex;
        } finally {
            sample.stop(filterTimer);
        }
    }

    private Bandwidth limitFor(String path) {

        if (path.startsWith("/api/stripe/webhook")) {
            return Bandwidth.builder()
                    .capacity(props.getWebhookCapacity())
                    .refillIntervally(
                            props.getWebhookRefillTokens(),
                            Duration.ofSeconds(props.getWebhookRefillSeconds())
                    )
                    .build();
        }

        if (path.startsWith("/api/admin")) {
            return Bandwidth.builder()
                    .capacity(props.getUserCapacity())
                    .refillIntervally(
                            props.getUserRefillTokens(),
                            Duration.ofSeconds(props.getUserRefillSeconds())
                    )
                    .build();
        }

        return Bandwidth.builder()
                .capacity(props.getPublicCapacity())
                .refillIntervally(
                        props.getPublicRefillTokens(),
                        Duration.ofSeconds(props.getPublicRefillSeconds())
                )
                .build();
    }

    private String resolveKey(HttpServletRequest request, String path) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return "rl:user:" + auth.getName() + ":" + pathGroup(path);
        }

        String ip = extractClientIp(request);
        return "rl:ip:" + ip + ":" + pathGroup(path);
    }

    private String pathGroup(String path) {
        if (path.startsWith("/api/stripe/webhook")) return "stripe-webhook";
        if (path.startsWith("/api/admin")) return "admin";
        if (path.startsWith("/api/orders")) return "orders";
        return "public";
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
