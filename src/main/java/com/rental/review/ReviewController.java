package com.rental.review;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

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

    // 리뷰 삭제
    @DeleteMapping("/{reviewId}/delete")
    public ResponseEntity<?> deleteReview(@PathVariable Long reviewId, @RequestParam Long memberId) {
        return ResponseEntity.ok(reviewService.deleteReview(reviewId, memberId));
    }
}
