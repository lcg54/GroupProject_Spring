package com.rental.dto;

import com.rental.constant.RentalStatus;
import com.rental.entity.Member;
import com.rental.entity.RentalItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RentalResponse {
    private Long id;
    private LocalDateTime createdAt;
    private RentalStatus status;
    private LocalDate rentalStart;
    private LocalDate rentalEnd;
    // 서비스 출장 날짜 기록용 필드 추가 필요
    private int rentalPeriodYears;
    private int monthlyPrice;
    private int totalPrice;
}
