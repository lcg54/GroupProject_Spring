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
                        sub.getStatus().name(),
                        sub.getRentalItem().getPaymentStatus().name()
                ));
    }

    // 구독 생성
    @Transactional
    public Subscription createSubscription(Long memberId, Long rentalItemId, String billingKey, int amount, LocalDate firstBillingDate) {
        RentalItem item = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("대여 상품 정보를 찾을 수 없습니다."));

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

        // 실제 결제 시도
        chargeSubscription(sub);
    }

    // 구독 취소: 예약 취소 시에 동작
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독 정보를 찾을 수 없습니다."));

        RentalItem item = rentalItemRepository.findById(sub.getRentalItem().getId())
                .orElseThrow(() -> new IllegalArgumentException("대여 상품 정보를 찾을 수 없습니다."));

        sub.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(sub);

        // 최근 결제 성공 기록 조회 — 수정: success = true 고정 메서드 사용
        paymentRecordRepository
                .findTopBySubscriptionIdAndSuccessTrueOrderByCreatedAtDesc(subscriptionId)
                .ifPresent(record -> { // 있으면 환불
                    try {
                        refundPayment(record.getPaymentKey(), record.getAmount(), item);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    // 실제 결제 시도
    @Transactional
    public void chargeSubscription(Subscription sub) {
        if (sub.getStatus() != SubscriptionStatus.ACTIVE) return;

        RentalItem item = rentalItemRepository.findById(sub.getRentalItem().getId())
                .orElseThrow(() -> new IllegalArgumentException("대여 상품 정보를 찾을 수 없습니다."));

        // 결제 시도 실패 시
        if (sub.getRetryCount() == 0) {
            // 첫 결제 실패 → 예약 취소
            item.setStatus(RentalStatus.CANCELED);
            item.setPaymentStatus(PaymentStatus.UNPAID);
            rentalItemRepository.save(item);

            // 이미 결제된 금액이 있다면 환불
            paymentRecordRepository.findAll().stream()
                    .filter(r -> Objects.equals(r.getSubscriptionId(), sub.getId()) && r.isSuccess())
                    .max(Comparator.comparing(PaymentRecord::getCreatedAt))
                    .ifPresent(record -> refundPayment(record.getPaymentKey(), record.getAmount(), item));

            return; // 추가 재시도 없이 종료
        }

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
    private void refundPayment(String paymentKey, int amount, RentalItem item) {
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

        // 환불 완료 상태 업데이트
        item.setPaymentStatus(PaymentStatus.REFUNDED);
        rentalItemRepository.save(item);
    }
}
