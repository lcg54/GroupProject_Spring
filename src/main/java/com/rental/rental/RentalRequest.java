package com.rental.rental;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RentalRequest {
    private Long memberId; // 주문자 ID
    private List<RentalItemRequest> items; // 대여상품 리스트

    @Data
    public static class RentalItemRequest {
        private Long productId;      // 상품 ID
        private int quantity;        // 수량
        private int periodYears;     // 대여기간(년)
        private LocalDate rentalStart; // 대여 시작일
    }
}