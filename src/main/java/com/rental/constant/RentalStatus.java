package com.rental.constant;

// 고객이 선택한 상품 낱개의 대여 상태
public enum RentalStatus {
    RESERVED,    // 예약만 된 상태(고객이 예약 완료)
    SHIPPING,    // 배송 중(픽업/배송 진행)
    RENTED,      // 실제 사용 중 (대여 시작)
    RETURNED,    // 반납 완료
    LATE,        // 연체
    REPAIR,      // 수리 중
    CANCELED     // 예약 취소
}

