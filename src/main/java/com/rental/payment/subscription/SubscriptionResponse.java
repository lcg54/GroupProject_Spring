package com.rental.payment.subscription;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private Long rentalItemId;
    private String productName;
    private int amount;
    private LocalDate nextBillingDate;
    private String status;
    private String paymentStatus;
}
