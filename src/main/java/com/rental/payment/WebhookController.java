package com.rental.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class WebhookController { // 토스가 전송하는 이벤트(청구 성공/실패 등)를 수신하면 보완/동기화용으로 사용

    // Toss 이벤트 수신 -> 로깅/추후 처리
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Toss-Signature", required = false) String signature
    ) {
        // 서명 검증 생략함 (실제 운영 시 반드시 검증 필요)
        System.out.println("🟪 Toss Webhook payload: " + payload);
        // 파싱 후 PaymentRecord & Subscription 갱신 로직 추가 가능

        return ResponseEntity.ok("ok");
    }
}
