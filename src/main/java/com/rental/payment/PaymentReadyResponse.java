package com.rental.payment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaymentReadyResponse {
    private String orderId;
    private int amount;
    private String customerName;
    private List<PaymentReadyRequest.Item> items;
}