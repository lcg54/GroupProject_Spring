package com.rental.service;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.entity.Product;
import com.rental.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Page<Product> getFilteredProducts(Category category, Brand brand, Boolean available, String keyword, String sortBy, int page, int size) {
        // 정렬 기준 설정
        Sort sort = Sort.unsorted();
        if ("POPULAR".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "rentedStock"); // 많이 대여된 순
        } else if ("PRICE_ASC".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.ASC, "pricePerPeriod");
        } else if ("PRICE_DESC".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "pricePerPeriod");
        }

        Pageable pageable = PageRequest.of(page - 1, size, sort);
        return productRepository.findFilteredProducts(category, brand, available, keyword, pageable);
    }
}
