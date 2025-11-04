package com.rental.controller;

import com.rental.dto.ReviewResponse;
import com.rental.entity.Review;
import com.rental.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    // 상품별 리뷰 조회
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getReviews(
            @RequestParam Long productId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "latest") String sortOrder
    ) {
        return ResponseEntity.ok(reviewService.getReviews(productId, memberId, page, size, sortOrder));
    }

    // 리뷰 추천
    @PostMapping("/recommend")
    public ResponseEntity<?> toggleRecommend(@RequestBody Map<String, Long> request) {
        Long reviewId = request.get("reviewId");
        Long memberId = request.get("memberId");
        var result = reviewService.toggleRecommend(reviewId, memberId);
        return ResponseEntity.ok(result);
    }

    // 회원별 리뷰 조회
    @GetMapping("/member")
    public ResponseEntity<Map<String, Object>> getMemberReviews(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "latest") String sortOrder
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByMember(memberId, page, size, sortOrder));
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}/delete")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId, @RequestParam Long memberId) {
        return ResponseEntity.ok(reviewService.deleteReview(reviewId, memberId));
    }

    // 리뷰 등록
    @PostMapping("/create")
    public ResponseEntity<?> createReview(@RequestBody Map<String, Object> req) {
        Long rentalItemId = ((Number) req.get("rentalItemId")).longValue();
        Long memberId = ((Number) req.get("memberId")).longValue();
        double rating = ((Number) req.get("rating")).doubleValue();
        String title = (String) req.get("title");
        String content = (String) req.get("content");
        List<String> imageFileNames = (List<String>) req.get("images");

        Review saved = reviewService.createReview(rentalItemId, memberId, rating, title, content, imageFileNames);
        return ResponseEntity.ok(saved);
    }

    // 리뷰 수정
    @PutMapping("/update/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> req
    ) {
        double rating = ((Number) req.get("rating")).doubleValue();
        String title = (String) req.get("title");
        String content = (String) req.get("content");
        List<String> imageFileNames = (List<String>) req.get("images");

        Review updated = reviewService.updateReview(reviewId, rating, title, content, imageFileNames);
        return ResponseEntity.ok(ReviewResponse.from(updated, false));
    }

    // 리뷰 단건 조회
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long reviewId) {
        Review review = reviewService.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        return ResponseEntity.ok(ReviewResponse.from(review, false));
    }

    // 특정 회원이 특정 상품에 대해 이미 작성한 리뷰 조회
    @GetMapping("/member/{memberId}/product/{productId}")
    public ResponseEntity<ReviewResponse> getMemberReviewByProduct(
            @PathVariable Long memberId,
            @PathVariable Long productId
    ) {
        Review review = reviewService.findByMemberAndProduct(memberId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 없습니다."));
        return ResponseEntity.ok(ReviewResponse.from(review, false));
    }

}
