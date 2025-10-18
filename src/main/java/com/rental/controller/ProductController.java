package com.rental.controller;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.dto.ProductResponse;
import com.rental.entity.Product;
import com.rental.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // 상품 목록 조회
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getProducts(
            @RequestParam(required = false) List<Category> category,
            @RequestParam(required = false) List<Brand> brand,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<Product> productPage = productService.getFilteredProducts(category, brand, available, keyword, sortBy, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("products", productPage.getContent());
        result.put("currentPage", productPage.getNumber() + 1);
        result.put("totalPages", productPage.getTotalPages());
        result.put("totalItems", productPage.getTotalElements());
        return ResponseEntity.ok(result);
    }

    // 상품 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.findById(id);
        return ResponseEntity.ok(product);
    }
}
