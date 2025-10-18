package com.rental.service;

import com.rental.entity.*;
import com.rental.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final RentalItemRepository rentalItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

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
        Review review = Review.builder().product(rentalItem.getProduct()).member(member).rentalItem(rentalItem).rating(rating).title(title).content(content).build();

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

    // 상품별 리뷰 조회
    public List<Review> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    // 회원별 리뷰 조회
    public List<Review> getReviewsByMember(Long memberId) {
        return reviewRepository.findByMemberId(memberId);
    }

    // 리뷰 상세 조회
    public Review getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
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

        return reviewRepository.save(review);
    }

    // 리뷰 삭제
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        reviewRepository.delete(review);
    }
}
