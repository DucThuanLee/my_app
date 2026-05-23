package de.thfamily18.restaurant_backend.service.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class PayPalAuthClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${paypal.base-url}")
    private String baseUrl;

    @Value("${paypal.client-id}")
    private String clientId;

    @Value("${paypal.client-secret}")
    private String clientSecret;

    public String getAccessToken() {
        var body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type", "client_credentials");

        PayPalTokenResponse res = restClientBuilder
                .baseUrl(baseUrl)
                .build()
                .post()
                .uri("/v1/oauth2/token")
                .headers(h -> h.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(PayPalTokenResponse.class);

        if (res == null || res.accessToken() == null || res.accessToken().isBlank()) {
            throw new IllegalStateException("Cannot obtain PayPal access token");
        }

        return res.accessToken();
    }

    public record PayPalTokenResponse(
            @JsonProperty("access_token")
            String accessToken
    ) {}
}
