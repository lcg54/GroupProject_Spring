package com.rental.payment.subscription;

import com.rental.constant.SubscriptionStatus;
import com.rental.rental.RentalItem;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_item_id", nullable = false)
    private RentalItem rentalItem;

    private String billingKey; // 암호화 적용 가능

    private int amount; // 매회 청구 금액 (monthlyPrice * qty)

    private LocalDate nextBillingDate;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private int retryCount; // 현재 재시도 횟수 (0 ~ 3)

    private LocalDateTime createdAt;
    private LocalDateTime lastChargedAt;

    @PrePersist
    private void pre() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
