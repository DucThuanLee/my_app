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

    private static final String STRIPE_WEBHOOK_PATH =
            "/api/payments/stripe/webhook";

    private final RateLimitProperties props;
    private final MeterRegistry meterRegistry;

    // In-memory buckets.
    // Fine for local development / single-instance demo deployments.
    private final ConcurrentHashMap<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    private Counter allowedCounter;
    private Counter blockedCounter;
    private Counter errorCounter;
    private Timer filterTimer;

    @PostConstruct
    void initMetrics() {
        this.allowedCounter =
                meterRegistry.counter("ratelimit.allowed");

        this.blockedCounter =
                meterRegistry.counter("ratelimit.blocked");

        this.errorCounter =
                meterRegistry.counter("ratelimit.errors");

        this.filterTimer =
                meterRegistry.timer("ratelimit.filter.time");

        Gauge.builder(
                        "ratelimit.buckets",
                        buckets,
                        ConcurrentHashMap::size
                )
                .description(
                        "Number of in-memory rate-limit buckets"
                )
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            String path = request.getRequestURI();

            /*
             * Do not rate-limit Stripe webhooks.
             *
             * Stripe can retry or send multiple events in a short burst.
             * Blocking them with HTTP 429 can delay payment/refund
             * synchronization.
             *
             * Webhook authenticity is verified separately using the
             * Stripe-Signature header and webhook signing secret.
             */
            if (STRIPE_WEBHOOK_PATH.equals(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Health checks should not consume application rate-limit tokens.
            if (path.startsWith("/actuator")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Swagger/OpenAPI documentation.
            if (path.startsWith("/swagger")
                    || path.startsWith("/v3/api-docs")) {

                filterChain.doFilter(request, response);
                return;
            }

            String key = resolveKey(request, path);

            Bucket bucket = buckets.computeIfAbsent(
                    key,
                    ignored -> Bucket.builder()
                            .addLimit(limitFor(path))
                            .build()
            );

            ConsumptionProbe probe =
                    bucket.tryConsumeAndReturnRemaining(1);

            response.setHeader(
                    "X-RateLimit-Remaining",
                    String.valueOf(probe.getRemainingTokens())
            );

            if (!probe.isConsumed()) {
                blockedCounter.increment();

                long waitSeconds = Math.max(
                        1,
                        probe.getNanosToWaitForRefill()
                                / 1_000_000_000L
                );

                response.setHeader(
                        "Retry-After",
                        String.valueOf(waitSeconds)
                );

                response.setStatus(
                        HttpStatus.TOO_MANY_REQUESTS.value()
                );

                response.setContentType(
                        "application/json"
                );

                response.getWriter().write(
                        """
                        {
                          "status": 429,
                          "error": "TOO_MANY_REQUESTS",
                          "message": "Rate limit exceeded"
                        }
                        """
                );

                return;
            }

            allowedCounter.increment();

            filterChain.doFilter(request, response);

        } catch (Exception ex) {
            errorCounter.increment();
            throw ex;

        } finally {
            sample.stop(filterTimer);
        }
    }

    private Bandwidth limitFor(String path) {

        if (path.startsWith("/api/admin")) {
            return Bandwidth.builder()
                    .capacity(props.getUserCapacity())
                    .refillIntervally(
                            props.getUserRefillTokens(),
                            Duration.ofSeconds(
                                    props.getUserRefillSeconds()
                            )
                    )
                    .build();
        }

        return Bandwidth.builder()
                .capacity(props.getPublicCapacity())
                .refillIntervally(
                        props.getPublicRefillTokens(),
                        Duration.ofSeconds(
                                props.getPublicRefillSeconds()
                        )
                )
                .build();
    }

    private String resolveKey(
            HttpServletRequest request,
            String path
    ) {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null
                && !"anonymousUser".equals(auth.getName())) {

            return "rl:user:"
                    + auth.getName()
                    + ":"
                    + pathGroup(path);
        }

        String ip = extractClientIp(request);

        return "rl:ip:"
                + ip
                + ":"
                + pathGroup(path);
    }

    private String pathGroup(String path) {

        if (path.startsWith("/api/admin")) {
            return "admin";
        }

        if (path.startsWith("/api/orders")) {
            return "orders";
        }

        if (path.startsWith("/api/payments")) {
            return "payments";
        }

        if (path.startsWith("/api/auth")) {
            return "auth";
        }

        return "public";
    }

    private String extractClientIp(
            HttpServletRequest request
    ) {

        String xForwardedFor =
                request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null
                && !xForwardedFor.isBlank()) {

            return xForwardedFor
                    .split(",")[0]
                    .trim();
        }

        return request.getRemoteAddr();
    }
}
