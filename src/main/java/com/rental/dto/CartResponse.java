package com.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class CartResponse {
    private Long id;
    private LocalDateTime createdAt;
    private List<CartItemResponse> items;

    @Data
    @AllArgsConstructor
    public static class CartItemResponse {
        private Long productId;
        private String productName;
        private int quantity;
        private int pricePerUnit;
        private int periodYears;
        private LocalDate rentalStart;
        private int estimatedPrice;
    }
}