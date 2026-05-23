package de.thfamily18.restaurant_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayPalConfig {

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    @Value("${paypal.environment}")
    private String environment;

//    @Bean
//    public PayPalHttpClient payPalHttpClient() {
//
//        PayPalEnvironment env = "production".equalsIgnoreCase(environment)
//                ? new PayPalEnvironment.Live(clientId, clientSecret)
//                : new PayPalEnvironment.Sandbox(clientId, clientSecret);
//
//        return new PayPalHttpClient(env);
//    }
}
