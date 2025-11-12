package com.rental.payment.billingKey;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {
    Optional<BillingKey> findByMemberId(Long memberId);

    List<BillingKey> findByCustomerKey(String customerKey);
}