package com.rental.repository;

import com.rental.constant.RentalStatus;
import com.rental.entity.RentalItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {
    Page<RentalItem> findByStatusOrderByRentalEndAsc(RentalStatus status, Pageable pageable);

    long countByStatus(RentalStatus status);
}
