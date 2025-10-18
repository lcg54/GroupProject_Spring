package com.rental.repository;

import com.rental.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductId(Long productId);

    List<Review> findByMemberId(Long memberId);

    Optional<Review> findByRentalItemId(Long rentalItemId);

    boolean existsByRentalItemId(Long rentalItemId);
}