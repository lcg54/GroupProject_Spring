package com.rental.repository;

import com.rental.entity.ReviewRecommend;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRecommendRepository extends JpaRepository<ReviewRecommend, Long> {
    Optional<ReviewRecommend> findByMemberIdAndReviewId(Long memberId, Long reviewId);
    List<ReviewRecommend> findByMemberId(Long memberId);
}