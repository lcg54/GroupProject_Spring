package com.rental.review;

import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String memberName;
    private String title;
    private String content;
    private double rating;
    private int recommend;
    private boolean recommended;
    private String regDate;
    private List<String> imageUrls;
    private Long rentalItemId;
    private Integer rentalPeriodYears;
    private String brand;
    private String mainImage;

    public static ReviewResponse from(Review review, boolean recommended) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .memberName(review.getMember() != null ? review.getMember().getName() : "탈퇴한 회원")
                .title(review.getTitle())
                .content(review.getContent())
                .rating(review.getRating())
                .recommend(review.getRecommend())
                .recommended(recommended)
                .regDate(review.getRegDate() != null ? review.getRegDate().toString() : null)
                .imageUrls(review.getImages() != null
                        ? review.getImages().stream()
                        .map(ReviewImage::getFileName)
                        .collect(Collectors.toList())
                        : null)
                .rentalItemId(review.getRentalItem() != null ? review.getRentalItem().getId() : null)
                .rentalPeriodYears(review.getRentalItem() != null ? review.getRentalItem().getRentalPeriodYears() : null)
                .brand(review.getProduct() != null && review.getProduct().getBrand() != null
                        ? review.getProduct().getBrand().name() : null)
                .mainImage(review.getProduct() != null ? review.getProduct().getMainImage() : null)
                .build();
    }
}
