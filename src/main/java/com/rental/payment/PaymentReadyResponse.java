package com.rental.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PaymentReadyResponse {
    private String orderId;
    private int amount;
    private String customerName;
}