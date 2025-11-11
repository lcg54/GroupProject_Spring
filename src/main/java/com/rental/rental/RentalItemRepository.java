package com.rental.rental;

import com.rental.constant.Category;
import com.rental.constant.RentalStatus;
import com.rental.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RentalItemRepository extends JpaRepository<RentalItem, Long> {
    List<RentalItem> findByRental(Rental rental);

    Page<RentalItem> findByStatus(RentalStatus status, Pageable pageable);

    long countByStatus(RentalStatus status);

    boolean existsByProduct(Product product);

    @Query("select ri from RentalItem ri " +
            "join ri.product p " +
            "where ri.rentalStart <= :endDate and ri.rentalEnd >= :startDate " +
            "and (:category is null or p.category = :category) " +
            "and (:rentalPeriod is null or ri.rentalPeriodYears = :rentalPeriod)")
    List<RentalItem> findItemsOverlappingRangeWithFilters(@Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate,
                                                          @Param("category") Category category,
                                                          @Param("rentalPeriod") Integer rentalPeriod);
}
