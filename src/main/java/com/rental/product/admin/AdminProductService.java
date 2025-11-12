package com.rental.product.admin;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.product.*;
import com.rental.rental.RentalItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductService {
    private final ProductRepository productRepository;
    private final ProductLogRepository productLogRepository;
    private final MemberRepository memberRepository;
    private final RentalItemRepository rentalItemRepository;

    @Value("${productImageLocation}")
    private String uploadDir;

    // 상품 등록
    public void register(
            Long memberId,
            List<MultipartFile> images,
            String name,
            String category,
            String brand,
            String description,
            Integer price,
            Boolean available,
            Integer totalStock
    ) throws IOException {
        Member admin = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

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

        productLogRepository.save(
                ProductLog.builder()
                        .product(product)
                        .member(admin)
                        .event("CREATE")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    // 상품 수정
    public void updateProduct(
            Long memberId,
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
            List<String> existingImages
    ) throws IOException {
        Member admin = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

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

        productLogRepository.saveAndFlush(
                ProductLog.builder()
                        .product(product)
                        .member(admin)
                        .event("UPDATE")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }

    // 상품 삭제
    @Transactional
    public void deleteProduct(Long id, Long memberId) {

        Member admin = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + id));

        boolean inUse = rentalItemRepository.existsByProduct(product);
        if (inUse) {
            throw new IllegalStateException("주문이 들어온 상품은 삭제 할 수 없습니다.");
        }

        product.delete();
        productRepository.save(product);

        productLogRepository.save(
                ProductLog.builder()
                        .product(product)
                        .member(admin)
                        .event("DELETE")
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}