package com.rental.service;

import com.rental.dto.ReviewResponse;
import com.rental.entity.*;
import com.rental.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final RentalItemRepository rentalItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ReviewRecommendRepository reviewRecommendRepository;

    // 상품별 리뷰 조회
    public Map<String, Object> getReviews(Long productId, Long memberId, int page, int size, String sortOrder) {
        Sort sort = switch(sortOrder) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "regDate");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "regDate");
            case "high" -> Sort.by(Sort.Direction.DESC, "rating");
            case "low" -> Sort.by(Sort.Direction.ASC, "rating");
            case "recommend" -> Sort.by(Sort.Direction.DESC, "recommend");
            default -> Sort.by(Sort.Direction.DESC, "recommend");
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);

        final Set<Long> recommendedReviewIds = (memberId != null)
                ? reviewRecommendRepository.findByMemberId(memberId)
                .stream()
                .map(rr -> rr.getReview().getId())
                .collect(Collectors.toSet())
                : Collections.emptySet();

        List<ReviewResponse> responses = reviewPage.getContent()
                .stream()
                .map(r -> ReviewResponse.from(r, recommendedReviewIds.contains(r.getId())))
                .collect(Collectors.toList());

        // 평균 평점 계산
        List<Review> allReviews = reviewRepository.findByProductId(productId);
        double avgRating = allReviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        // 평점별 개수
        int[] ratingCounts = new int[5];
        for (Review r : allReviews) {
            int rounded = (int) Math.round(r.getRating());
            if (rounded >= 1 && rounded <= 5) {
                ratingCounts[rounded - 1]++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", responses);
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("pageNumber", reviewPage.getNumber());
        response.put("averageRating", avgRating);
        response.put("ratingCounts", ratingCounts);

        return response;
    }

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
