package com.rental.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.dto.ProductResponse;
import com.rental.entity.Product;
import com.rental.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
            @RequestParam(defaultValue = "12") int size
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


    // 상품 등록
    @PostMapping(
            value = "/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponse> register(
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam String brand,
            @RequestParam(required = false) String description,
            @RequestParam Integer price,
            @RequestParam(defaultValue = "true") Boolean available,
            @RequestParam Integer totalStock
    ) throws IOException {
        ProductResponse saved = productService.register(images, name, category, brand, description, price, available, totalStock
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 상품 수정
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam String brand,
            @RequestParam(required = false) String description,
            @RequestParam Integer price,
            @RequestParam(defaultValue = "true") Boolean available,
            @RequestParam Integer totalStock,
            @RequestParam(name = "reservedStock", defaultValue = "0") Integer reservedStock,
            @RequestParam(name = "shippingStock", defaultValue = "0") Integer shippingStock,
            @RequestParam(name = "rentedStock",   defaultValue = "0") Integer rentedStock,
            @RequestParam(name = "repairStock",   defaultValue = "0") Integer repairStock,
            @RequestParam(name = "existingImages", required = false, defaultValue = "[]") String existingImages
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> existing = (existingImages == null || existingImages.isBlank())
                    ? Collections.emptyList()
                    : mapper.readValue(existingImages, new TypeReference<List<String>>() {
            });
            productService.updateProduct(id, images, name, category, brand, description, price, available, totalStock, reservedStock, shippingStock, rentedStock, repairStock, existing);
            return ResponseEntity.ok("상품 수정 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", "상품 수정 실패: " + e.getMessage()));
        }
    }

    // 상품 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try{
            productService.deleteProduct(id);
            return ResponseEntity.ok(java.util.Map.of("message", "상품 삭제 완료"));
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", "상품 삭제 실패" + e.getMessage()));
        }
    }
}