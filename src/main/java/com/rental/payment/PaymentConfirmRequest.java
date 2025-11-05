package com.rental.payment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentConfirmRequest {
    private String paymentKey;
    private String orderId;
    private int amount;
}