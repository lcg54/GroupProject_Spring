package com.rental.dto;

import lombok.Data;

@Data
public class RentalRequest {
    private Long memberId;
    private Long productId;
    private int periodYears;
}