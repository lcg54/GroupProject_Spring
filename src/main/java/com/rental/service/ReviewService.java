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
        Sort sort = sortOrder.equals("latest")
                ? Sort.by(Sort.Direction.DESC, "regDate")
                : Sort.by(Sort.Direction.ASC, "regDate");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);
        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("content", responses);
        response.put("totalElements", reviewPage.getTotalElements());
        response.put("totalPages", reviewPage.getTotalPages());
        response.put("pageNumber", reviewPage.getNumber());
        return response;
    }
}
