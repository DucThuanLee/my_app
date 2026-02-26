package de.thfamily18.restaurant_backend.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ratelimit")
public class RateLimitProperties {

    // Public endpoints: 60 req / minute / IP
    private long publicCapacity = 60;
    private long publicRefillTokens = 60;
    private long publicRefillSeconds = 60;

    // Stripe webhook stricter: 30 req / minute / IP
    private long webhookCapacity = 60;
    private long webhookRefillTokens = 60;
    private long webhookRefillSeconds = 60;

    // Authenticated user endpoints: 120 req / minute / user
    private long userCapacity = 60;
    private long userRefillTokens = 60;
    private long userRefillSeconds = 60;
}
