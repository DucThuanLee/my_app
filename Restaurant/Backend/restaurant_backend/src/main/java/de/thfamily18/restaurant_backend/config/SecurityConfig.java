package de.thfamily18.restaurant_backend.config;

import de.thfamily18.restaurant_backend.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // to use @PreAuthorize
//@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        /// ===== Public (no auth) =====
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()

                        // Products public
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // Guest checkout: create order
                        .requestMatchers(HttpMethod.POST, "/api/orders").permitAll()

                        // Stripe: webhook + create intent (guest can pay)
                        .requestMatchers(HttpMethod.POST, "/api/payments/stripe/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/stripe/intents").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/payments/stripe/status/**").permitAll() // if you use polling

                        // ===== Admin =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ===== Everything else =====
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            res.setStatus(HttpStatus.UNAUTHORIZED.value());
                            res.setContentType("application/json");
                            res.getWriter().write("""
                                        {
                                          "status": 401,
                                          "error": "UNAUTHORIZED",
                                          "message": "Authentication required",
                                          "path": "%s"
                                        }
                                    """.formatted(req.getRequestURI()));
                        })
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
