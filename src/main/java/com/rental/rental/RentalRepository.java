package com.rental.rental;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    /**
     * 회원의 Rental들을 member/items/product를 함께 fetch 하여 N+1 문제 방지.
     * 필요에 따라 서비스에서 이 메서드를 사용하세요.
     */
    @Query("SELECT DISTINCT r FROM Rental r " +
            "LEFT JOIN FETCH r.member m " +
            "LEFT JOIN FETCH r.items i " +
            "LEFT JOIN FETCH i.product p " +
            "WHERE m.id = :memberId " +
            "ORDER BY r.createdAt DESC")
    List<Rental> findRentalsByMemberId(@Param("memberId") Long memberId);
}