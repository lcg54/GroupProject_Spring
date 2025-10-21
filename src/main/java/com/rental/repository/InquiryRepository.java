package com.rental.repository;

import com.rental.entity.Inquiry;
import com.rental.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findByProduct(Product product);
    List<Inquiry> findByMemberId(Long memberId);

    boolean existsByMemberIdAndRentalItemId(Long memberId, Long rentalItemId);
}
