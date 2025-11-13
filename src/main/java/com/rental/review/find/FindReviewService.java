package com.rental.review.find;

import com.rental.review.Review;
import com.rental.review.ReviewRepository;
import com.rental.review.ReviewResponse;
import com.rental.review.recommend.ReviewRecommendRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewRecommendRepository reviewRecommendRepository;

    // 상품별 리뷰 조회
    public Map<String, Object> getReviews(Long productId, Long memberId, int page, int size, String sortOrder) {
        Sort sort = getSort(sortOrder);
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

        List<Review> allReviews = reviewRepository.findByProductId(productId);
        ReviewStats stats = calculateReviewStats(allReviews);

        Map<String, Object> response = new HashMap<>();
        response.put("content", responses);
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("pageNumber", reviewPage.getNumber());
        response.put("averageRating", stats.getAverageRating());
        response.put("ratingCounts", stats.getRatingCounts());

        return response;
    }

    // 회원별 리뷰 조회
    public Map<String, Object> getReviewsByMember(Long memberId, int page, int size, String sortOrder) {
        Sort sort = getSort(sortOrder);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Review> reviewPage = reviewRepository.findByMemberId(memberId, pageable);

        List<ReviewResponse> responses = reviewPage.getContent()
                .stream()
                .map(r -> ReviewResponse.from(r, false)) // 회원 본인 리뷰 조회이므로 추천 여부는 false로 초기화
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", responses);
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("pageNumber", reviewPage.getNumber());

        return response;
    }

    // 정렬용 내부 메서드
    private Sort getSort(String sortOrder) {
        return switch (sortOrder) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "regDate");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "regDate");
            case "high" -> Sort.by(Sort.Direction.DESC, "rating");
            case "low" -> Sort.by(Sort.Direction.ASC, "rating");
            case "recommend" -> Sort.by(Sort.Direction.DESC, "recommend");
            default -> Sort.by(Sort.Direction.DESC, "recommend");
        };
    }

    // 별점 계산용 내부 메서드
    private ReviewStats calculateReviewStats(List<Review> reviews) {
        double avgRating = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        int[] ratingCounts = new int[5];
        for (Review r : reviews) {
            int idx = Math.min(4, (int) Math.round(r.getRating()) - 1);
            ratingCounts[idx]++;
        }

        return new ReviewStats(avgRating, ratingCounts);
    }

    // 별점 응답용 내부 dto
    @Getter
    @AllArgsConstructor
    private static class ReviewStats {
        private final double averageRating;
        private final int[] ratingCounts;
    }

    // 리뷰 단건 조회
    public Optional<Review> findById(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }

    // 특정 회원이 특정 상품에 대해 이미 작성한 리뷰 조회
    public Optional<Review> findByMemberAndProduct(Long memberId, Long productId) {
        return reviewRepository.findByMemberIdAndProductId(memberId, productId);
    }
}
