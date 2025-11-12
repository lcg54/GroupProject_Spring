package com.rental.payment.subscription;

import com.rental.constant.PaymentStatus;
import com.rental.constant.RentalStatus;
import com.rental.constant.SubscriptionStatus;
import com.rental.payment.billingKey.BillingKeyRepository;
import com.rental.rental.RentalItem;
import com.rental.rental.RentalItemRepository;
import com.rental.rental.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final RentalItemRepository rentalItemRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final RentalRepository rentalRepository;

    @Value("${toss.secret-key}")
    private String secretKey;

    private static final int MAX_RETRY = 3;

    // 구독 조회
    public Optional<SubscriptionResponse> findSubscriptionByRentalItemId(Long rentalItemId) {
        return subscriptionRepository.findByRentalItemId(rentalItemId)
                .map(sub -> new SubscriptionResponse(
                        sub.getId(),
                        sub.getRentalItem().getId(),
                        sub.getRentalItem().getProduct().getName(),
                        sub.getAmount(),
                        sub.getNextBillingDate(),
                        sub.getStatus().name()
                ));
    }

    // 구독 생성
    @Transactional
    public Subscription createSubscription(Long memberId, Long rentalItemId, String billingKey, int amount, LocalDate firstBillingDate) {
        RentalItem item = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("rentalItem not found"));

        Subscription sub = Subscription.builder()
                .memberId(memberId)
                .rentalItem(item)
                .billingKey(billingKey)
                .amount(amount)
                .nextBillingDate(firstBillingDate)
                .status(SubscriptionStatus.ACTIVE)
                .retryCount(0)
                .build();

        return subscriptionRepository.save(sub);
    }

    // 당월 요금 즉시납부
    @Transactional
    public void chargeNow(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

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

        // 실제 결제 시도
        chargeSubscription(sub);
    }

    // 구독 취소: 대여 취소 시에 동작
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found"));

        boolean hadSuccess = paymentRecordRepository.findAll().stream()
                .anyMatch(r -> Objects.equals(r.getSubscriptionId(), subscriptionId) && r.isSuccess());

        sub.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(sub);

        // 결제 이력이 있으면 환불 시도
        if (hadSuccess) {
            paymentRecordRepository.findAll().stream()
                    .filter(r -> Objects.equals(r.getSubscriptionId(), subscriptionId) && r.isSuccess())
                    .max(Comparator.comparing(PaymentRecord::getCreatedAt))
                    .ifPresent(record -> {
                        try {
                            refundPayment(record.getPaymentKey(), record.getAmount());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    // 실제 결제 시도
    @Transactional
    public void chargeSubscription(Subscription sub) {
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) return;

        RentalItem item = rentalItemRepository.findById(sub.getRentalItem().getId())
                .orElseThrow(() -> new IllegalArgumentException("rentalItem not found"));

        // 반납/취소된 상품은 결제 중단
        if (item.getStatus() == RentalStatus.CANCELED ||
                item.getStatus() == RentalStatus.RETURNED ||
                item.getStatus() == RentalStatus.RETURN_REQUESTED) {

            sub.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(sub);
            return;
        }

        String url = "https://api.tosspayments.com/v1/billing/" + sub.getBillingKey();

        Map<String, Object> body = new HashMap<>();
        body.put("customerKey", sub.getMemberId().toString());
        body.put("amount", sub.getAmount());
        body.put("orderId", "rentalItem-" + item.getId() + "-" + System.currentTimeMillis());
        body.put("orderName", item.getProduct().getName());

        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        String auth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
        headers.set("Authorization", "Basic " + auth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = rt.postForEntity(url, req, Map.class);

            // 결제 성공 기록
            PaymentRecord record = PaymentRecord.builder()
                    .subscriptionId(sub.getId())
                    .rentalItemId(item.getId())
                    .amount(sub.getAmount())
                    .paymentKey(Objects.toString(resp.getBody().get("paymentKey"), ""))
                    .rawResponse(resp.getBody().toString())
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

        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            handleChargeFailure(sub, item, e.getResponseBodyAsString());
        } catch (Exception ex) {
            handleChargeFailure(sub, item, ex.getMessage());
        }
    }

    // 결제 실패 시 재시도 및 상태 변경
    private void handleChargeFailure(Subscription sub, RentalItem item, String errorMsg) {
        sub.setRetryCount(sub.getRetryCount() + 1);

        if (sub.getRetryCount() >= MAX_RETRY) {
            sub.setStatus(SubscriptionStatus.FAILED);
            item.setPaymentStatus(PaymentStatus.LATE);
            rentalItemRepository.save(item);
        } else {
            sub.setNextBillingDate(sub.getNextBillingDate().plusDays(1));
        }

        PaymentRecord record = PaymentRecord.builder()
                .subscriptionId(sub.getId())
                .rentalItemId(item.getId())
                .amount(sub.getAmount())
                .paymentKey(null)
                .rawResponse(errorMsg)
                .success(false)
                .build();
        paymentRecordRepository.save(record);

        subscriptionRepository.save(sub);
    }

    // 환불 API 호출
    private void refundPayment(String paymentKey, int amount) {
        if (paymentKey == null || paymentKey.isBlank()) return;

        String url = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        String auth = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
        headers.set("Authorization", "Basic " + auth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("cancelReason", "예약 취소에 따른 환불");
        body.put("cancelAmount", amount);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        rt.postForEntity(url, req, Map.class);
    }
}