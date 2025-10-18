package com.rental.repository;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p " +
            "WHERE ((:categories IS NULL OR p.category IN :categories)) " + //"WHERE (:category IS NULL OR p.category = :category) "
            "AND ((:brands IS NULL OR p.brand IN :brands)) " + //"AND (:brand IS NULL OR p.brand = :brand) "
            "AND (:available IS NULL OR " +
            "     (:available = TRUE AND (p.totalStock - p.reservedStock - p.rentedStock - p.repairStock) > 0) OR " +
            "     (:available = FALSE AND (p.totalStock - p.reservedStock - p.rentedStock - p.repairStock) <= 0)) " +
            "AND (:keyword IS NULL OR " +
            "     LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Product> findFilteredProducts(
            @Param("categories") List<Category> categories,
            @Param("brands") List<Brand> brands,
            @Param("available") Boolean available,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
