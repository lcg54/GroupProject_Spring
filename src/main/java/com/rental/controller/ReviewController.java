package com.rental.controller;

import com.rental.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "latest") String sortOrder
    ) {
        return ResponseEntity.ok(reviewService.getReviews(productId, page, size, sortOrder));
    }
}
