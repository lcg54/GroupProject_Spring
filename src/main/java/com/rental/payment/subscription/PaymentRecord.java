package com.rental.payment.subscription;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long subscriptionId;
    private Long rentalItemId;

    private int amount;
    private String paymentKey; // Toss가 준 paymentKey / transaction id
    private String rawResponse; // JSON 문자열 전체 응답 (테스트용)

    private boolean success;
    private LocalDateTime createdAt;

    @PrePersist
    private void pre() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}