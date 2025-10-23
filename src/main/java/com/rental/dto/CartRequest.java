package com.rental.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CartRequest {
    private Long memberId;
    private List<CartItemRequest> items;

    @Data
    public static class CartItemRequest {
        private Long productId;
        private int quantity;
        private int periodYears;
        private LocalDate rentalStart;
    }
}