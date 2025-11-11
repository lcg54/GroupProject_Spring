package com.rental.payment.subscription;

import com.rental.constant.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    // 매일 00:05 KST 에 실행
    // 테스트 시에는 cron을 0 */1 * * * * (매분) 등으로 바꿔 빠르게 확인하기
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void dailyCharge() {
        LocalDate today = LocalDate.now();
        List<Subscription> subs = subscriptionRepository.findByNextBillingDateAndStatus(today, SubscriptionStatus.ACTIVE);

        for (Subscription s : subs) {
            try {
                subscriptionService.chargeSubscription(s);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}