package com.rental.repository;

import com.rental.constant.RentalStatus;
import com.rental.entity.Product;
import com.rental.entity.RentalItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {
    Page<RentalItem> findByStatus(RentalStatus status, Pageable pageable);

    long countByStatus(RentalStatus status);

    List<RentalItem> findByRentalStartBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByProduct(Product product);
}
