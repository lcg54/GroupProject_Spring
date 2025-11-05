package com.rental.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // (1) 결제 준비 - 주문 정보 생성
    @PostMapping("/ready")
    public ResponseEntity<?> readyPayment(@RequestBody PaymentReadyRequest request) {
        String username = request.getUsername();
        PaymentReadyResponse response = paymentService.preparePayment(request, username);
        return ResponseEntity.ok(response);
    }

    // (2) 결제 승인
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(@RequestBody PaymentConfirmRequest request) {
        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }
}