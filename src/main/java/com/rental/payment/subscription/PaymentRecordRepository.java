package com.rental.payment.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    // success = true 인 최근 결제 기록 1개 조회
    Optional<PaymentRecord> findTopBySubscriptionIdAndSuccessTrueOrderByCreatedAtDesc(Long subscriptionId);
}
