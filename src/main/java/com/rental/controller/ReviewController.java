package com.rental.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.dto.ReviewResponse;
import com.rental.entity.Review;
import com.rental.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;

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

        List<String> imageFileNames = Optional.ofNullable(req.get("images"))
                .map(obj -> objectMapper.convertValue(obj, new TypeReference<List<String>>() {}))
                .orElse(Collections.emptyList());

        Review updated = reviewService.updateReview(reviewId, rating, title, content, imageFileNames);
        return ResponseEntity.ok(ReviewResponse.from(updated, false));
    }
}
