package com.rental.dto;

import com.rental.constant.RentalStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class RentalResponse {
    private Long id;
    private RentalStatus status;
    private LocalDateTime createdAt;
    private int totalPrice;
    private List<RentalItemResponse> items;

    @Data
    @AllArgsConstructor
    public static class RentalItemResponse {
        private Long productId;
        private String productName;
        private int quantity;
        private int pricePerUnit;
        private int rentalPeriodYears;
        private LocalDate rentalStart;
        private LocalDate rentalEnd;
        private int itemTotalPrice;
    }
}