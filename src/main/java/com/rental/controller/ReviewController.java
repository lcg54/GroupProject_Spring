package com.rental.controller;

import com.rental.entity.Review;
import com.rental.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 등록
    @PostMapping
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

    // 상품별 리뷰 조회
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getReviewsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    // 회원별 리뷰 조회
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Review>> getReviewsByMember(@PathVariable Long memberId) {
        return ResponseEntity.ok(reviewService.getReviewsByMember(memberId));
    }

    // 리뷰 상세 조회
    @GetMapping("/{reviewId}")
    public ResponseEntity<Review> getReview(@PathVariable Long reviewId) {
        return ResponseEntity.ok(reviewService.getReview(reviewId));
    }

    // 리뷰 수정
    @PutMapping("/{reviewId}")
    public ResponseEntity<Review> updateReview(
            @PathVariable Long reviewId,
            @RequestBody Map<String, Object> req
    ) {
        double rating = ((Number) req.get("rating")).doubleValue();
        String title = (String) req.get("title");
        String content = (String) req.get("content");
        List<String> imageFileNames = (List<String>) req.get("images");

        Review updated = reviewService.updateReview(reviewId, rating, title, content, imageFileNames);
        return ResponseEntity.ok(updated);
    }

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(Map.of("message", "리뷰가 삭제되었습니다."));
    }
}
