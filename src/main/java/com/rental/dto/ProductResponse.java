package com.rental.dto;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
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
}
