package com.rental.service;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.dto.ProductResponse;
import com.rental.entity.Product;
import com.rental.entity.ProductImage;
import com.rental.entity.ProductLog;
import com.rental.entity.Review;
import com.rental.repository.ProductLogRepository;
import com.rental.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductLogRepository productLogRepository;

    @Value("${productImageLocation}")
    private String uploadDir;

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
        return productRepository.findByCategoryImageContaining(keyword);
    }

    // 상품 등록
    public ProductResponse register(
            List<MultipartFile> images,
            String name,
            String category,
            String brand,
            String description,
            Integer price,
            Boolean available,
            Integer totalStock,
            String adminName
    ) throws IOException {

        if (uploadDir == null || uploadDir.isBlank()) {
            throw new IllegalStateException("productImageLocation(업로드 경로)가 설정되지 않았습니다.");
        }
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("이미지는 최소 1개 이상 필요합니다.");
        }

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        List<ProductImage> imageEntities = new ArrayList<>();
        String mainImageFile = null;
        int seq = 1;

        for (MultipartFile imageFile : images) {
            if (imageFile == null || imageFile.isEmpty()) continue;

            String original = Optional.ofNullable(imageFile.getOriginalFilename()).orElse("img");
            String ext = original.contains(".") ? original.substring(original.lastIndexOf(".")) : "";
            String saved = UUID.randomUUID().toString() + ext;

            imageFile.transferTo(new File(dir, saved));

            if (mainImageFile == null) mainImageFile = saved;
            imageEntities.add(new ProductImage(null, null, saved, null, seq++));
        }

        if (mainImageFile == null) {
            throw new IllegalArgumentException("업로드된 유효한 이미지가 없습니다.");
        }

        Product product = new Product();
        product.setName(name);
        product.setCategory(Category.valueOf(category.toUpperCase()));
        product.setBrand(Brand.valueOf(brand.toUpperCase()));
        product.setDescription(description);
        product.setPrice(price);
        product.setTotalStock(totalStock);
        product.setReservedStock(0);
        product.setShippingStock(0);
        product.setRentedStock(0);
        product.setRepairStock(0);
        product.setReturnRequestedStock(0);
        product.setAvailable(available != null ? available : true);
        product.setRegDate(LocalDate.now());
        product.setMainImage(mainImageFile);

        for (ProductImage pi : imageEntities) {
            pi.setProduct(product);
        }
        product.setImages(imageEntities);

        productRepository.save(product);

        String finalAdmin = (adminName != null && !adminName.isBlank()) ? adminName : "관리자";

        productLogRepository.save(
                ProductLog.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .adminName(finalAdmin)
                        .event("CREATE")
                        .build()
        );

        return convertToResponse(product);
    }

    // 상품 수정
    public void updateProduct(
            Long id,
            List<MultipartFile> images,
            String name,
            String category,
            String brand,
            String description,
            Integer price,
            Boolean available,
            Integer totalStock,
            Integer reservedStock,
            Integer shippingStock,
            Integer rentedStock,
            Integer repairStock,
            List<String> existingImages,
            String adminName
    ) throws IOException {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 상품입니다."));

        product.setName(name);
        product.setCategory(Category.valueOf(category.toUpperCase()));
        product.setBrand(Brand.valueOf(brand.toUpperCase()));
        product.setDescription(description);
        product.setPrice(price);
        product.setAvailable(available != null ? available : true);
        product.setTotalStock(totalStock);

        product.setReservedStock(Optional.ofNullable(reservedStock).orElse(0));
        product.setShippingStock(Optional.ofNullable(shippingStock).orElse(0));
        product.setRentedStock(Optional.ofNullable(rentedStock).orElse(0));
        product.setRepairStock(Optional.ofNullable(repairStock).orElse(0));

        if (product.getImages() == null) product.setImages(new ArrayList<>());
        List<String> keep = (existingImages == null) ? Collections.emptyList() : existingImages;
        product.getImages().removeIf(pi -> !keep.contains(pi.getFileName()));

        if (images != null && !images.isEmpty()) {
            if (uploadDir == null || uploadDir.isBlank()) {
                throw new IllegalStateException("productImageLocation(업로드 경로)가 설정되지 않았습니다.");
            }
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            int nextSeq = product.getImages().stream()
                    .mapToInt(ProductImage::getSeq)
                    .max().orElse(0) + 1;

            for (MultipartFile imageFile : images) {
                if (imageFile == null || imageFile.isEmpty()) continue;

                String original = Optional.ofNullable(imageFile.getOriginalFilename()).orElse("img");
                String ext = original.contains(".") ? original.substring(original.lastIndexOf(".")) : "";
                String saved = UUID.randomUUID().toString() + ext;

                imageFile.transferTo(new File(dir, saved));
                product.getImages().add(new ProductImage(null, product, saved, null, nextSeq++));
            }
        }

        if (product.getImages().isEmpty()) {
            throw new IllegalArgumentException("상품 이미지는 최소 1개 이상이어야 합니다.");
        }

        String newMain = product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getSeq))
                .map(ProductImage::getFileName)
                .findFirst()
                .orElse(null);
        if (newMain != null) {
            product.setMainImage(newMain);
        } else if (product.getMainImage() == null && !product.getImages().isEmpty()) {
            product.setMainImage(product.getImages().get(0).getFileName());
        } else if (product.getMainImage() == null) {
            throw new IllegalArgumentException("대표 이미지를 찾을 수 없습니다.");
        }

        productRepository.save(product);

        String finalAdmin = (adminName != null && !adminName.isBlank()) ? adminName : "관리자";
        productLogRepository.save(
                ProductLog.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .adminName(finalAdmin)
                        .event("UPDATE")
                        .build()
        );
    }

    // 상품 삭제
    public void deleteProduct(Long id, String adminName) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 상품입니다."));

        String finalAdmin = (adminName != null && !adminName.isBlank()) ? adminName : "관리자";

        productLogRepository.save(
                ProductLog.builder()
                        .productId(product.getId())
                        .productName(product.getName())
                        .adminName(finalAdmin)
                        .event("DELETE")
                        .build()
        );

        productRepository.delete(product);
    }
}