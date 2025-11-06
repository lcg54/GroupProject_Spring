package com.rental.repository;


import com.rental.entity.ProductLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductLogRepository extends JpaRepository<ProductLog, Long> {
    List<ProductLog> findByEventInOrderByCreatedAtDesc(Collection<String> events);
    @EntityGraph(attributePaths = "member")
    List<ProductLog> findByEventOrderByCreatedAtDesc(String event);
}