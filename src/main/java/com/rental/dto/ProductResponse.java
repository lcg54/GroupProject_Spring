package com.rental.dto;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private Brand brand;
    private Category category;
    private int price;
    private int totalStock;
    private int reservedStock;
    private int rentedStock;
    private int repairStock;
    private String mainImage;
    private String description;
    private List<String> images;

    private double averageRating; // 평균 평점
    private int reviewCount;      // 리뷰 개수
}
