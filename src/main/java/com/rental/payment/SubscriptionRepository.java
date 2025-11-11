package com.rental.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByNextBillingDateAndStatus(LocalDate date, com.rental.constant.SubscriptionStatus status);
    Optional<Subscription> findByRentalItemId(Long rentalItemId);
}