package com.rental.service;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.dto.ProductResponse;
import com.rental.entity.Product;
import com.rental.entity.ProductImage;
import com.rental.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    // 상품 목록 조회
    public Page<ProductResponse> getFilteredProducts(List<Category> categories, List<Brand> brands, Boolean available, String keyword, String sortBy, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Product> products = productRepository.findFilteredProducts(
                (categories == null || categories.isEmpty()) ? null : categories,
                (brands == null || brands.isEmpty()) ? null : brands,
                available,
                (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim(),
                sortBy,
                pageable
        );
        return products.map(this::convertToResponse);
    }

    // 단일 상품 조회
    public ProductResponse findById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 상품입니다."));
        return convertToResponse(p);
    }

    // 인기 상품 Top3
    public List<ProductResponse> getPopularProducts() {
        return productRepository.findTop3ByOrderByRentedStockDesc()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // Entity → DTO 변환
    private ProductResponse convertToResponse(Product p) {
        List<String> imageFileNames = (p.getImages() == null) ? List.of() :
                p.getImages().stream()
                        .sorted((a, b) -> Integer.compare(a.getSeq(), b.getSeq()))
                        .map(ProductImage::getFileName)
                        .collect(Collectors.toList());
        return new ProductResponse(p.getId(), p.getName(), p.getBrand(), p.getCategory(), p.getPrice(), p.getTotalStock(), p.getReservedStock(), p.getRentedStock(), p.getRepairStock(), p.getMainImage(), p.getDescription(), imageFileNames);
    }
}