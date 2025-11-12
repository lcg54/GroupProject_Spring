package com.rental.review.recommend;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewRecommendController {
    private final ReviewRecommendService reviewRecommendService;

    // 리뷰 추천
    @PostMapping("/recommend")
    public ResponseEntity<?> toggleRecommend(@RequestBody Map<String, Long> request) {
        Long reviewId = request.get("reviewId");
        Long memberId = request.get("memberId");
        var result = reviewRecommendService.toggleRecommend(reviewId, memberId);
        return ResponseEntity.ok(result);
    }
}
