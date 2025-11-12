package com.rental.review.find;

import com.rental.review.Review;
import com.rental.review.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class FindReviewController {
    private final FindReviewService findReviewService;

    // 상품별 리뷰 조회
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getReviews(
            @RequestParam Long productId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size,
            @RequestParam(defaultValue = "latest") String sortOrder
    ) {
        return ResponseEntity.ok(findReviewService.getReviews(productId, memberId, page, size, sortOrder));
    }

    // 회원별 리뷰 조회
    @GetMapping("/member")
    public ResponseEntity<Map<String, Object>> getMemberReviews(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "latest") String sortOrder
    ) {
        return ResponseEntity.ok(findReviewService.getReviewsByMember(memberId, page, size, sortOrder));
    }

    // 리뷰 단건 조회
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long reviewId) {
        Review review = findReviewService.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        return ResponseEntity.ok(ReviewResponse.from(review, false));
    }

    // 특정 회원이 특정 상품에 대해 이미 작성한 리뷰 조회
    @GetMapping("/member/{memberId}/product/{productId}")
    public ResponseEntity<ReviewResponse> getMemberReviewByProduct(
            @PathVariable Long memberId,
            @PathVariable Long productId
    ) {
        Review review = findReviewService.findByMemberAndProduct(memberId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰가 없습니다."));
        return ResponseEntity.ok(ReviewResponse.from(review, false));
    }
}
