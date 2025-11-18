package com.rental.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    @EntityGraph(attributePaths = {"product", "member"})
    Page<Inquiry> findByProductId(Long productId, Pageable pageable);

    @EntityGraph(attributePaths = {"product", "member"})
    Page<Inquiry> findByMemberId(Long memberId, Pageable pageable);

    // 답변 완료된 문의
    @EntityGraph(attributePaths = {"product", "member", "adminComment", "adminComment.admin"})
    Page<Inquiry> findByAdminCommentIsNotNull(Pageable pageable);

    // 답변 대기 중인 문의
    @EntityGraph(attributePaths = {"product", "member"})
    Page<Inquiry> findByAdminCommentIsNull(Pageable pageable);

    // 전체 조회 오버라이드
    @EntityGraph(attributePaths = {"product", "member", "adminComment", "adminComment.admin"})
    @Override
    Page<Inquiry> findAll(Pageable pageable);

    // 미답변 문의 수
    @Query("SELECT COUNT(i) FROM Inquiry i WHERE i.adminComment IS NULL")
    long countByNoAdminComment();
}