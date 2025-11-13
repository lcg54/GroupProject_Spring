package com.rental.constant;

public enum PaymentStatus {
    PAID,       // 결제 완료
    LATE,       // 연체 (결제 실패)
    UNPAID,     // 결제 전 (초기 상태)
    REFUNDED,    // 환불 완료
    END         // 만료 (대여 기간 종료)
}