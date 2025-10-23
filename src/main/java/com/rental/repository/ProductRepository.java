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
    @Query("""
        SELECT p FROM Product p
        LEFT JOIN Review r ON r.product = p
        WHERE ((:categories IS NULL OR p.category IN :categories))
        AND ((:brands IS NULL OR p.brand IN :brands))
        AND (:available IS NULL OR 
             (:available = TRUE AND (p.totalStock - p.reservedStock - p.rentedStock - p.repairStock) > 0) OR 
             (:available = FALSE AND (p.totalStock - p.reservedStock - p.rentedStock - p.repairStock) <= 0))
        AND (:keyword IS NULL OR 
             LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR 
             LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        GROUP BY p
        ORDER BY
          CASE WHEN :sortBy = 'POPULAR' THEN p.rentedStock END DESC,
          CASE WHEN :sortBy = 'PRICE_ASC' THEN p.price END ASC,
          CASE WHEN :sortBy = 'PRICE_DESC' THEN p.price END DESC,
          CASE WHEN :sortBy = 'RATING_DESC' THEN AVG(r.rating) END DESC,
          p.id DESC
    """)
    Page<Product> findFilteredProducts(
            @Param("categories") List<Category> categories,
            @Param("brands") List<Brand> brands,
            @Param("available") Boolean available,
            @Param("keyword") String keyword,
            @Param("sortBy") String sortBy,
            Pageable pageable
    );

    List<Product> findTop3ByOrderByRentedStockDesc();

    List<Product> findByCategoryImageContaining(String keyword);
}
