package com.rental.controller;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.dto.ProductResponse;
import com.rental.entity.Product;
import com.rental.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @Value("${productImageLocation}")
    private String productImageLocation ; // 기본 값 : null

    // 상품 목록 조회
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(required = false) List<Category> category,
            @RequestParam(required = false) List<Brand> brand,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ProductResponse> productPage = productService.getFilteredProducts(category, brand, available, keyword, sortBy, page, size);

        Map<String, Object> result = new HashMap<>();
        result.put("products", productPage.getContent());
        result.put("currentPage", productPage.getNumber() + 1);
        result.put("totalPages", productPage.getTotalPages());
        result.put("totalItems", productPage.getTotalElements());

        return ResponseEntity.ok(result);
    }

    // 상품 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    // 인기상품 Top3
    @GetMapping("/popular")
    public ResponseEntity<List<ProductResponse>> getPopularProducts() {
        return ResponseEntity.ok(productService.getPopularProducts());
    }

    // 카테고리 사진
    @GetMapping("/category/images")
    public ResponseEntity<List<Map<String, String>>> getCategoryImages() {
        List<Product> products = productService.findCategoryImage("category");
        String baseUrl = "http://localhost:9000/images/";

        List<Map<String, String>> result = products.stream()
                .map(p -> Map.of(
                        "category", p.getCategory().name(),
                        "categoryImage", baseUrl + p.getCategoryImage()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }
}