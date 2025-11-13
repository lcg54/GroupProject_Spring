package com.rental.product;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.review.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;

    @Value("${productImageSubLocation}")
    private String subUpload;

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
        return productRepository.findTop3ByDeletedFalseOrderByRentedStockDesc()
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
        // 리뷰 정보
        double averageRating = 0.0;
        int reviewCount = 0;

        if (p.getReviews() != null && !p.getReviews().isEmpty()) {
            reviewCount = p.getReviews().size();
            averageRating = p.getReviews().stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);
        }
        return new ProductResponse(
                p.getId(), p.getName(), p.getBrand(), p.getCategory(), p.getPrice(), p.getTotalStock(), p.getReservedStock(), p.getShippingStock(), p.getRentedStock(), p.getRepairStock(), p.getReturnRequestedStock(), p.getAvailableStock(), p.getMainImage(), p.getDescription(),  imageFileNames, averageRating, reviewCount
        );
    }

    // 카테고리 사진
    public List<Product> findCategoryImage(String keyword) {
        return productRepository.findByDeletedFalseAndCategoryImageContaining(keyword);
    }

    // 상품 정보 + 이미지 통합 조회
    public Map<String, Object> getProductWithImages(Long id) {
        ProductResponse product = findById(id);
        if (product == null)
            throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + id);

        // 카테고리명과 상품명으로 폴더 구조 찾기
        String categoryName = product.getCategory().name();
        String productFolder = findProductFolderName(categoryName, product.getName());

        // 이미지 분류
        Map<String, List<String>> imageMap = getProductImages(categoryName, productFolder);

        // 응답 맵 구성
        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("images", imageMap);
        return result;
    }

    // 상품명 기반 폴더 탐색
    private String findProductFolderName(String categoryName, String productName) {
        File categoryDir = new File(subUpload, categoryName);
        if (!categoryDir.exists() || !categoryDir.isDirectory())
            throw new IllegalStateException("카테고리 폴더 없음: " + categoryDir.getAbsolutePath());

        File[] matches = categoryDir.listFiles(f ->
                f.isDirectory() && f.getName().startsWith(productName + "_")
        );

        if (matches == null || matches.length == 0)
            throw new IllegalStateException("상품 폴더를 찾을 수 없음: " + productName);

        return matches[0].getName();
    }

    // 이미지 파일 분류 (main, sub, detail)
    private Map<String, List<String>> getProductImages(String categoryName, String productFolder) {
        File productDir = new File(subUpload, categoryName + "/" + productFolder);
        if (!productDir.exists() || !productDir.isDirectory())
            throw new IllegalStateException("상품 폴더가 존재하지 않음: " + productDir.getAbsolutePath());

        File[] files = productDir.listFiles((dir, name) -> name.matches("(?i).+\\.(jpg|jpeg|png|gif)$"));
        if (files == null || files.length == 0)
            throw new IllegalStateException("이미지 없음: " + productDir.getAbsolutePath());

        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

        Map<String, List<String>> result = new HashMap<>();
        List<String> main = new ArrayList<>();
        List<String> sub = new ArrayList<>();
        List<String> detail = new ArrayList<>();

        for (File f : files) {
            String name = f.getName().toLowerCase();
            // 프론트가 접근 가능한 URL로 반환
            String path = "/images/category/" + categoryName + "/" + productFolder + "/" + f.getName();

            if (name.startsWith("main_")) main.add(path);
            else if (name.startsWith("sub_")) sub.add(path);
            else detail.add(path);
        }

        result.put("main", main);
        result.put("sub", sub);
        result.put("detail", detail);
        return result;
    }
}