package com.rental.review;

import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.rental.RentalItem;
import com.rental.rental.RentalItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final RentalItemRepository rentalItemRepository;
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
        Review review = Review.builder().product(rentalItem.getProduct()).member(member).rentalItem(rentalItem).rating(rating).title(title).content(content).regDate(LocalDateTime.now()).build();

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

        if (review.getRentalItem() != null) {
            RentalItem item = review.getRentalItem();
            if (item.getProduct() == null) {
                item.setProduct(review.getProduct());
            }
        }
        return reviewRepository.save(review);
    }

    // 리뷰 삭제
    @Transactional
    public String deleteReview(Long reviewId, Long memberId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 리뷰 작성자와 로그인한 유저가 일치하는지
        if (!review.getMember().getId().equals(memberId)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        reviewRepository.delete(review);
        return "리뷰가 삭제되었습니다.";
    }
}
