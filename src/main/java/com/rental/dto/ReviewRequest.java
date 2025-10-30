package com.rental.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {
    private Long rentalItemId;
    private Long memberId;
    private double rating;
    private String title;
    private String content;
    private List<String> images;
}