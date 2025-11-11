package com.rental.payment.instant;

import lombok.Data;

import java.util.List;

@Data
public class PaymentReadyRequest {
    private String username;
    private int totalAmount;
    private List<Item> items;

    @Data
    public static class Item {
        private Long productId;
        private int quantity;
        private int periodYears;
    }
}