package com.rental.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    Page<Inquiry> findByProductId(Long productId, Pageable pageable);
    Page<Inquiry> findByMemberId(Long memberId, Pageable pageable);

    // 답변 완료된 문의
    Page<Inquiry> findByAdminCommentIsNotNull(Pageable pageable);

    // 답변 대기 중인 문의
    Page<Inquiry> findByAdminCommentIsNull(Pageable pageable);
}