package com.rental.payment;

import com.rental.payment.billingKey.BillingKey;
import com.rental.payment.billingKey.BillingKeyRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final BillingKeyRepository billingKeyRepository;

    // 구독 조회
    @GetMapping("/by-item/{rentalItemId}")
    public ResponseEntity<?> getSubscription(@PathVariable Long rentalItemId) {
        return subscriptionService.findSubscriptionByRentalItemId(rentalItemId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 구독 생성
    @PostMapping("/create")
    public ResponseEntity<Subscription> createSubscription(@RequestBody CreateSubscriptionRequest req) {
        BillingKey bk = billingKeyRepository.findByMemberId(req.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("billing key를 찾을 수 없습니다."));

        Subscription s = subscriptionService.createSubscription(
                req.getMemberId(),
                req.getRentalItemId(),
                bk.getBillingKey(),
                req.getAmount(),
                req.getFirstBillingDate()
        );
        return ResponseEntity.ok(s);
    }

    // 당월 요금 즉시납부
    @PostMapping("/charge-now/{subscriptionId}")
    public ResponseEntity<String> chargeNow(@PathVariable Long subscriptionId) {
        subscriptionService.chargeNow(subscriptionId);
        return ResponseEntity.ok("charged");
    }

    @Data
    public static class CreateSubscriptionRequest {
        private Long memberId;
        private Long rentalItemId;
        private int amount;
        private LocalDate firstBillingDate;
    }
}