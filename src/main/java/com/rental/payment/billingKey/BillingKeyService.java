package com.rental.payment.billingKey;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BillingKeyService {
    private final BillingKeyRepository billingKeyRepository;

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

    public List<Map<String, Object>> getCardsByCustomerKey(String customerKey) {
        List<BillingKey> billingKeys = billingKeyRepository.findByCustomerKey(customerKey);

        return billingKeys.stream().map(bk -> {
            Map<String, Object> cardInfo = fetchCardInfoFromToss(bk.getBillingKey());
            return Map.of(
                    "billingKey", bk.getBillingKey(),
                    "cardCompany", cardInfo.get("cardCompany"),
                    "lastFourDigits", cardInfo.get("lastFourDigits")
            );
        }).toList();
    }

    // Toss API 호출
    private Map<String, Object> fetchCardInfoFromToss(String billingKey) {
        try {
            String url = "https://api.tosspayments.com/v1/billing/authorizations/" + billingKey;

            HttpHeaders headers = new HttpHeaders();
            String encodedAuth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            // Toss API 반환 구조: body.get("card") 안에 카드 정보 있음
            if (body != null && body.containsKey("card")) {
                Map<String, Object> card = (Map<String, Object>) body.get("card");
                return Map.of(
                        "cardCompany", card.get("company"),
                        "lastFourDigits", card.get("number")  // 마지막 4자리
                );
            } else {
                return Map.of(
                        "cardCompany", "알 수 없음",
                        "lastFourDigits", "****"
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "cardCompany", "오류",
                    "lastFourDigits", "****"
            );
        }
    }
}
