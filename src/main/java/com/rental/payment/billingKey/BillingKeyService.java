package com.rental.payment.billingKey;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BillingKeyService {

    @Value("${toss.secret-key}")
    private String secretKey;

    // BillingKey 발급 요청
    public String requestBillingKey(BillingRequest request) {
        try {
            String url = "https://api.tosspayments.com/v1/billing/authorizations/" + request.getAuthKey();

            HttpHeaders headers = new HttpHeaders();
            String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("customerKey", request.getCustomerKey());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return (String) response.getBody().get("billingKey");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("빌링키 발급 실패: " + e.getMessage());
        }
    }
}
