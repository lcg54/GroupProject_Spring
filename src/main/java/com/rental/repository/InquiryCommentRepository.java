package com.rental.repository;

import com.rental.entity.InquiryComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryCommentRepository extends JpaRepository<InquiryComment, Long> {
    InquiryComment findByInquiryId(Long inquiryId);
}
