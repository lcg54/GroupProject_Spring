package com.rental.service;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.dto.ProductResponse;
import com.rental.entity.Product;
import com.rental.entity.ProductImage;
import com.rental.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    // 상품 목록 조회
    public Page<Product> getFilteredProducts(List<Category> categories, List<Brand> brands, Boolean available, String keyword, String sortBy, int page, int size) {
        try {
            // 정렬 설정
            Sort sort = Sort.unsorted();
            if ("POPULAR".equalsIgnoreCase(sortBy)) {
                sort = Sort.by(Sort.Direction.DESC, "rentedStock"); // 대여 기록 기준이어야 하는데 일단 대여중 기준으로 함
            } else if ("PRICE_ASC".equalsIgnoreCase(sortBy)) {
                sort = Sort.by(Sort.Direction.ASC, "price");
            } else if ("PRICE_DESC".equalsIgnoreCase(sortBy)) {
                sort = Sort.by(Sort.Direction.DESC, "price");
            }
            // 상품 목록 요청
            Pageable pageable = PageRequest.of(page - 1, size, sort);
            return productRepository.findFilteredProducts(
                    (categories == null || categories.isEmpty()) ? null : categories,
                    (brands == null || brands.isEmpty()) ? null : brands,
                    available,
                    (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim(),
                    pageable
            );
        } catch (Exception e) {
            throw new RuntimeException("상품 목록을 불러오는 중 오류가 발생했습니다.", e);
        }
    }

    public ProductResponse findById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 상품입니다."));
        List<String> imageFileNames = p.getImages().stream()
                .sorted((a, b) -> Integer.compare(a.getSeq(), b.getSeq()))
                .map(ProductImage::getFileName)
                .toList();
        return new ProductResponse(
                p.getId(), p.getName(), p.getBrand(), p.getCategory(), p.getPrice(), p.getTotalStock(), p.getReservedStock(), p.getRentedStock(), p.getRepairStock(), p.getMainImage(), p.getDescription(), imageFileNames
        );
    }
}
