package com.rental.payment.fake;

import com.rental.constant.PaymentStatus;
import com.rental.constant.RentalStatus;
import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.payment.billingKey.BillingKeyRepository;
import com.rental.payment.instant.PaymentConfirmRequest;
import com.rental.payment.instant.PaymentConfirmResponse;
import com.rental.payment.subscription.*;
import com.rental.product.Product;
import com.rental.product.ProductRepository;
import com.rental.rental.Rental;
import com.rental.rental.RentalItem;
import com.rental.rental.RentalItemRepository;
import com.rental.rental.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/fake/subscriptions")
@RequiredArgsConstructor
public class FakeSubController {
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final RentalItemRepository rentalItemRepository;

    @Value("${toss.secret-key}")
    private String secretKey;

    // 당월 요금 즉시납부
    @PostMapping("/charge-now/{subscriptionId}")
    public ResponseEntity<String> chargeNow(@PathVariable Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        // 이번 달 결제 여부 확인
        paymentRecordRepository.findAll().stream()
                .filter(r -> Objects.equals(r.getSubscriptionId(), subscriptionId) && r.isSuccess())
                .max(Comparator.comparing(PaymentRecord::getCreatedAt))
                .ifPresent(last -> {
                    LocalDate lastMonth = last.getCreatedAt().toLocalDate().withDayOfMonth(1);
                    LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
                    if (lastMonth.equals(thisMonth)) {
                        throw new IllegalStateException("이미 이번 달 결제가 완료되었습니다.");
                    }
                });

        RentalItem item = rentalItemRepository.findById(sub.getRentalItem().getId())
                .orElseThrow(() -> new IllegalArgumentException("대여 상품 정보를 찾을 수 없습니다."));

        // 결제 성공 기록
        PaymentRecord record = PaymentRecord.builder()
                .subscriptionId(sub.getId())
                .rentalItemId(item.getId())
                .amount(sub.getAmount())
                .paymentKey("paymentKey" + subscriptionId)
                .rawResponse("rawResponse")
                .success(true)
                .build();
        paymentRecordRepository.save(record);

        // 결제 성공 시 처리
        item.setPaymentStatus(PaymentStatus.PAID);
        rentalItemRepository.save(item);

        sub.setLastChargedAt(java.time.LocalDateTime.now());
        sub.setNextBillingDate(sub.getNextBillingDate().plusMonths(1));
        sub.setRetryCount(0);
        subscriptionRepository.save(sub);

        return ResponseEntity.ok("charged");
    }

    @PostMapping("/confirm/{subscriptionId}")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(
            @RequestBody PaymentConfirmRequest request,
            @PathVariable Long subscriptionId
    ) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        // 이번 달 결제 여부 확인
        paymentRecordRepository.findAll().stream()
                .filter(r -> Objects.equals(r.getSubscriptionId(), subscriptionId) && r.isSuccess())
                .max(Comparator.comparing(PaymentRecord::getCreatedAt))
                .ifPresent(last -> {
                    LocalDate lastMonth = last.getCreatedAt().toLocalDate().withDayOfMonth(1);
                    LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
                    if (lastMonth.equals(thisMonth)) {
                        throw new IllegalStateException("이미 이번 달 결제가 완료되었습니다.");
                    }
                });

        RentalItem item = rentalItemRepository.findById(sub.getRentalItem().getId())
                .orElseThrow(() -> new IllegalArgumentException("대여 상품 정보를 찾을 수 없습니다."));

        String url = "https://api.tosspayments.com/v1/payments/confirm";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(secretKey, "");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", request.getPaymentKey());
        body.put("orderId", request.getOrderId());
        body.put("amount", request.getAmount());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // 결제 성공 기록
        PaymentRecord record = PaymentRecord.builder()
                .subscriptionId(sub.getId())
                .rentalItemId(item.getId())
                .amount(sub.getAmount())
                .paymentKey("paymentKey" + subscriptionId)
                .rawResponse("rawResponse")
                .success(true)
                .build();
        paymentRecordRepository.save(record);

        // 결제 성공 시 처리
        item.setPaymentStatus(PaymentStatus.PAID);
        rentalItemRepository.save(item);

        sub.setLastChargedAt(java.time.LocalDateTime.now());
        sub.setNextBillingDate(sub.getNextBillingDate().plusMonths(1));
        sub.setRetryCount(0);
        subscriptionRepository.save(sub);

        return ResponseEntity.ok(new PaymentConfirmResponse("success", response.getBody()));
    }
}
