package com.rental.review;

import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.rental.RentalItem;
import com.rental.rental.RentalItemRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final RentalItemRepository rentalItemRepository;
    private final MemberRepository memberRepository;
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

    // 리뷰 삭제
    @Transactional
    public String deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 리뷰 작성자와 로그인한 유저가 일치하는지
        if (!review.getMember().getId().equals(memberId)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        reviewRepository.delete(review);
        return "리뷰가 삭제되었습니다.";
    }

    // 리뷰 등록
    @Transactional
    public Review createReview(Long rentalItemId, Long memberId, double rating, String title, String content, List<String> imageFileNames) {
        RentalItem rentalItem = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("대여상품 기록을 찾을 수 없습니다."));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        if (reviewRepository.existsByRentalItemId(rentalItemId)) {
            throw new IllegalStateException("이미 해당 대여상품에 대한 리뷰가 존재합니다.");
        }
        Review review = Review.builder().product(rentalItem.getProduct()).member(member).rentalItem(rentalItem).rating(rating).title(title).content(content).regDate(LocalDateTime.now()).build();

        // 이미지 등록 (수정할거)
        if (imageFileNames != null && !imageFileNames.isEmpty()) {
            for (int i = 0; i < imageFileNames.size(); i++) {
                ReviewImage img = ReviewImage.builder()
                        .review(review)
                        .fileName(imageFileNames.get(i))
                        .seq(i)
                        .build();
                review.getImages().add(img);
            }
        }

        // ReviewItem 매핑
        rentalItem.setReview(review);

        return reviewRepository.save(review);
    }

    // 리뷰 수정
    @Transactional
    public Review updateReview(Long reviewId, double rating, String title, String content, List<String> imageFileNames) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        review.setRating(rating);
        review.setTitle(title);
        review.setContent(content);

        // 기존 이미지 교체
        review.getImages().clear();
        if (imageFileNames != null && !imageFileNames.isEmpty()) {
            for (int i = 0; i < imageFileNames.size(); i++) {
                ReviewImage img = ReviewImage.builder()
                        .review(review)
                        .fileName(imageFileNames.get(i))
                        .seq(i)
                        .build();
                review.getImages().add(img);
            }
        }

        if (review.getRentalItem() != null) {
            RentalItem item = review.getRentalItem();
            if (item.getProduct() == null) {
                item.setProduct(review.getProduct());
            }
        }
        return reviewRepository.save(review);
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
