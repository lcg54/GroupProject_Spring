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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final RentalItemRepository rentalItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    // 상품별 리뷰 조회
    public Map<String, Object> getReviews(Long productId, int page, int size, String sortOrder) {
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

        List<ReviewResponse> responses = reviewPage.getContent()
                .stream()
                .map(ReviewResponse::from)
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
            int idx = Math.min(4, (int)Math.round(r.getRating()) - 1);
            ratingCounts[idx]++;
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
}
