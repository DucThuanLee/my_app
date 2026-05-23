package de.thfamily18.restaurant_backend.service.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayPalWebhookVerifier {

    private final RestClient.Builder restClientBuilder;
    private final PayPalAuthClient authClient;

    @Value("${paypal.base-url}")
    private String baseUrl;

    @Value("${paypal.webhook-id}")
    private String webhookId;

    public boolean verify(
            String transmissionId,
            String transmissionTime,
            String transmissionSig,
            String certUrl,
            String authAlgo,
            JsonNode webhookEvent
    ) {
        String accessToken = authClient.getAccessToken();

        Map<String, Object> body = Map.of(
                "transmission_id", transmissionId,
                "transmission_time", transmissionTime,
                "transmission_sig", transmissionSig,
                "cert_url", certUrl,
                "auth_algo", authAlgo,
                "webhook_id", webhookId,
                "webhook_event", webhookEvent
        );

        VerifyResponse res = restClientBuilder
                .baseUrl(baseUrl)
                .build()
                .post()
                .uri("/v1/notifications/verify-webhook-signature")
                .headers(h -> h.setBearerAuth(accessToken))
                .body(body)
                .retrieve()
                .body(VerifyResponse.class);

        return res != null && "SUCCESS".equalsIgnoreCase(res.verificationStatus());
    }

    public record VerifyResponse(
            @JsonProperty("verification_status")
            String verificationStatus
    ) {}
}
