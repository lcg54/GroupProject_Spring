package com.rental.constant;

// 고객이 선택한 상품의 대여 상태
public enum RentalStatus {
    RESERVED,    // 예약 중
    SHIPPING,    // 배송 중
    RENTED,      // 대여 중
    RETURN_REQUESTED, // 반납 요청 중
    RETURNED,    // 반납 완료
    LATE,        // 연체
    REPAIR,      // 수리 중
    CANCELED     // 예약 취소
}

