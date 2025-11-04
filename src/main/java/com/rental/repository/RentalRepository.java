package com.rental.repository;

import com.rental.entity.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    // 방법 1: 쿼리 메서드 (간단)
    List<Rental> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 방법 2: @Query 사용 (명확한 조인, N+1 문제 방지)
    @Query("SELECT r FROM Rental r " +
            "LEFT JOIN FETCH r.member m " +
            "LEFT JOIN FETCH r.items i " +
            "LEFT JOIN FETCH i.product " +
            "WHERE m.id = :memberId " +
            "ORDER BY r.createdAt DESC")
    List<Rental> findRentalsByMemberId(@Param("memberId") Long memberId);

    List<Rental> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}