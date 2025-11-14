package com.rental.payment.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findTopBySubscriptionIdAndSuccessOrderByCreatedAtDesc(Long subscriptionId);
}