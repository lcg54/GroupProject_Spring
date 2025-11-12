package com.rental.review.recommend;

import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.review.Review;
import com.rental.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewRecommendService {
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final ReviewRecommendRepository reviewRecommendRepository;

    // 리뷰 추천
    @Transactional
    public Map<String, Object> toggleRecommend(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리뷰입니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        var existing = reviewRecommendRepository.findByMemberIdAndReviewId(memberId, reviewId);

        boolean isRecommended;
        if (existing.isPresent()) {
            // 이미 추천한 경우 → 취소
            reviewRecommendRepository.delete(existing.get());
            review.setRecommend(Math.max(0, review.getRecommend() - 1));
            isRecommended = false;
        } else {
            // 추천하지 않은 경우 → 추가
            ReviewRecommend recommend = ReviewRecommend.builder()
                    .member(member)
                    .review(review)
                    .build();
            reviewRecommendRepository.save(recommend);
            review.setRecommend(review.getRecommend() + 1);
            isRecommended = true;
        }

        reviewRepository.save(review);

        Map<String, Object> response = new HashMap<>();
        response.put("reviewId", review.getId());
        response.put("recommend", review.getRecommend());
        response.put("isRecommended", isRecommended);

        return response;
    }
}
