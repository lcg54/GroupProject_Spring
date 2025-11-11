package com.rental.payment.instant;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // (1) 결제 준비 - Toss 결제용 orderId/amount/고객명 반환
    @PostMapping("/ready")
    public ResponseEntity<PaymentReadyResponse> readyPayment(@RequestBody PaymentReadyRequest request) {
        // username은 request에 포함되어 전달됨
        PaymentReadyResponse response = paymentService.preparePayment(request, request.getUsername());
        return ResponseEntity.ok(response);
    }

    // (2) 결제 승인 - 결제 성공 시 Rental/Item 생성
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(@RequestBody PaymentConfirmRequest request) {
        // username + items 정보 포함
        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }
}