package com.rental.review;

import com.rental.constant.RentalStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class RentalResponse {
    private Long id;  // 주문 ID
    private LocalDateTime createdAt;
    private int totalPrice;
    private List<RentalItemResponse> items;

    @Data
    @AllArgsConstructor
    public static class RentalItemResponse {
        private Long itemId;
        private Long productId;
        private String productName;
        private int quantity;
        private int pricePerUnit;
        private int rentalPeriodYears;
        private LocalDate rentalStart;
        private LocalDate rentalEnd;
        private int itemTotalPrice;
        private RentalStatus status;
        private String mainImage;
    }
}