package com.rental.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InquiryCommentRepository extends JpaRepository<InquiryComment, Long> {
    InquiryComment findByInquiryId(Long inquiryId);
}
