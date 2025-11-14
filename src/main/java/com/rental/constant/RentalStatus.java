package com.rental.constant;

public enum RentalStatus {
    RESERVED,    // 예약 중
    SHIPPING,    // 배송 중
    RENTED,      // 대여 중
    RETURN_REQUESTED, // 반납 요청 중
    RETURNED,    // 반납 완료
    REPAIR,      // 수리 중
    CANCELED     // 예약 취소
}