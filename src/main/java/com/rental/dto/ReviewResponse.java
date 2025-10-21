package com.rental.dto;

import com.rental.entity.Review;
import com.rental.entity.ReviewImage;
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
    private String memberName;
    private String productName;
    private String title;
    private String content;
    private double rating;
    private String regDate;
    private List<String> imageUrls;

    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .memberName(review.getMember() != null ? review.getMember().getName() : "탈퇴한 회원")
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .title(review.getTitle())
                .content(review.getContent())
                .rating(review.getRating())
                .regDate(review.getRegDate() != null ? review.getRegDate().toString() : null)
                .imageUrls(review.getImages() != null
                        ? review.getImages().stream()
                        .map(ReviewImage::getFileName)
                        .collect(Collectors.toList())
                        : null)
                .build();
    }
}
