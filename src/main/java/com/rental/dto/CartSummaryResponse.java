package com.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartSummaryResponse {
    private Long productId;
    private String productName;
    private String brand;
    private int price;
    private String imageUrl;
    private int totalQuantity;
}