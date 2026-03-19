package de.thfamily18.restaurant_backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        AntPathMatcher matcher = new AntPathMatcher();

        return matcher.match("/auth/**", path)
                || matcher.match("/swagger-ui/**", path)
                || matcher.match("/swagger-ui.html", path)
                || matcher.match("/v3/api-docs/**", path)
                || matcher.match("/api/products/**", path)
                || matcher.match("/api/orders", path)
                || matcher.match("/api/payments/stripe/webhook", path)
                || matcher.match("/api/payments/stripe/intents", path)
                || matcher.match("/api/payments/stripe/status/**", path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        log.info("doFilterInternal:");
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            log.info("doFilterInternal: no auth header or not Bearer");
            chain.doFilter(request, response);
            return;
        }
        log.info("doFilterInternal: Bearer");
        String token = auth.substring(7);

        try {
            Claims claims = jwtService.verifyAndGetClaims(token);
            String email = claims.getSubject();

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            chain.doFilter(request, response);
        } catch (Exception e) {

//            // Token invalid/expired -> return 401
//            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//            response.setContentType("application/json");
//            response.getWriter().write("{\"message\":\"Invalid or expired token\"}");

            // Do not set authentication => let the entrypoint handle 401 for the protected endpoint.
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }

//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String path = request.getServletPath();
//        return path.startsWith("/actuator/");
//    }
}
