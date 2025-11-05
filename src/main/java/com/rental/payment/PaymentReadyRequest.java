package com.rental.payment;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaymentReadyRequest {
    private String username;
    private List<Item> items;
    private int totalAmount;

    @Getter
    @Setter
    public static class Item {
        private Long productId;
        private int quantity;
        private int periodYears;
    }
}