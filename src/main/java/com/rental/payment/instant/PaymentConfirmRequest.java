package com.rental.payment.instant;

import lombok.Data;

import java.util.List;

@Data
public class PaymentConfirmRequest {
    private String paymentKey;
    private String orderId;
    private int amount;
    private String username;
    private List<Item> items;

    @Data
    public static class Item {
        private Long productId;
        private int quantity;
        private int periodYears;
    }
}