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
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private final ObjectMapper objectMapper;
    @Value("${productImageLocation}")
    private String uploadDir;

    // 상품 등록
    public void register(
            Long memberId,
            MultipartFile mainImage,
            List<MultipartFile> subImages,
            List<MultipartFile> detailImages,
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

        String categoryDirName = category.toUpperCase();
        String productDirName = name.replaceAll("[\\\\/:*?\"<>|]", "_") + "_" + UUID.randomUUID().toString().substring(0, 4);

        File productDir = new File(uploadDir, "category/" + categoryDirName + "/" + productDirName);

        if (!productDir.exists() && !productDir.mkdirs()) {
            throw new IllegalStateException("상품 폴더 생성 실패: " + productDir.getAbsolutePath());
        }

        List<ProductImage> imageEntities = new ArrayList<>();
        String mainImageFile = null;

        // 대표 이미지
        if (mainImage != null && !mainImage.isEmpty()) {
            String ext = Optional.ofNullable(mainImage.getOriginalFilename()).orElse("img").replaceAll(".*(\\.[^.]+)$", "$1");
            String savedName = "main_image" + ext;

            File dest = new File(productDir, savedName);
            mainImage.transferTo(dest);

            mainImageFile = "category/" + categoryDirName + "/" + productDirName + "/" + savedName;

            imageEntities.add(new ProductImage(null, null, mainImageFile, null, 0)); // seq 0: 대표 이미지
        } else {
            throw new IllegalArgumentException("대표 이미지는 필수입니다.");
        }

        // 서브 이미지 (최대 4장)
        if (subImages != null) {
            int seq = 1;
            for (MultipartFile sub : subImages) {
                if (sub == null || sub.isEmpty()) continue;
                if (seq > 4) break;

                String ext = Optional.ofNullable(sub.getOriginalFilename()).orElse("img").replaceAll(".*(\\.[^.]+)$", "$1");
                String savedName = "sub_image" + seq + ext;

                File dest = new File(productDir, savedName);
                sub.transferTo(dest);

                String path = "category/" + categoryDirName + "/" + productDirName + "/" + savedName;
                imageEntities.add(new ProductImage(null, null, path, null, seq));

                seq++;
            }
        }

        // 상세 이미지 (순서대로)
        if (detailImages != null) {
            int seq = 1;
            for (MultipartFile detail : detailImages) {
                if (detail == null || detail.isEmpty()) continue;

                String ext = Optional.ofNullable(detail.getOriginalFilename()).orElse("img").replaceAll(".*(\\.[^.]+)$", "$1");
                String savedName = seq + ext;

                File dest = new File(productDir, savedName);
                detail.transferTo(dest);

                String path = "category/" + categoryDirName + "/" + productDirName + "/" + savedName;
                imageEntities.add(new ProductImage(null, null, path, null, seq));

                seq++;
            }
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
    @Transactional
    public void updateProduct(
            Long memberId,
            Long id,
            MultipartFile mainImage,
            List<MultipartFile> subImages,
            List<MultipartFile> detailImages,
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
            List<String> keep,
            List<String> deleteMainSub,  // 삭제할 대표/서브 이미지 URL
            List<String> deleteDetail
    ) throws IOException {

        Member admin = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 상품입니다."));

        // 기본 정보 수정
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

        // 🔥 이미지 처리 시작 (기존 이미지 처리 로직 전부 교체)
        String categoryDirName = category.toUpperCase();
        String productDirName = name.replaceAll("[\\\\/:*?\"<>|]", "_")
                + "_" + UUID.randomUUID().toString().substring(0, 4);

        File newDir = new File(uploadDir + "/category/" + categoryDirName + "/" + productDirName);
        if (!newDir.exists()) newDir.mkdirs();

        List<ProductImage> images = new ArrayList<>();

        // 대표 및 서브 이미지 삭제 처리
        if (!deleteMainSub.isEmpty()) {
            for (String oldPath : deleteMainSub) {
                File src = new File(uploadDir + "/" + oldPath);
                if (src.exists()) {
                    src.delete(); // 파일 삭제
                }
            }
        }

        // 상세 이미지 삭제 처리
        if (!deleteDetail.isEmpty()) {
            for (String oldPath : deleteDetail) {
                File src = new File(uploadDir + "/" + oldPath);
                if (src.exists()) {
                    src.delete(); // 파일 삭제
                }
            }
        }

        // 3️⃣ 새 대표 이미지 저장
        String mainImagePath = null;
        if (mainImage != null && !mainImage.isEmpty()) {
            String ext = Optional.ofNullable(mainImage.getOriginalFilename())
                    .orElse("img").replaceAll(".*(\\.[^.]+)$", "$1");

            String savedName = "main_image" + ext;
            File dest = new File(newDir, savedName);
            mainImage.transferTo(dest);

            mainImagePath = "category/" + categoryDirName + "/" + productDirName + "/" + savedName;
            images.add(new ProductImage(null, product, mainImagePath, null, 0));
        }

        // 4️⃣ 새 서브 이미지 저장
        if (subImages != null) {
            int seq = 1;
            for (MultipartFile sub : subImages) {
                if (sub == null || sub.isEmpty()) continue;
                if (seq > 4) break;

                String ext = Optional.ofNullable(sub.getOriginalFilename())
                        .orElse("img").replaceAll(".*(\\.[^.]+)$", "$1");

                String savedName = "sub_image" + seq + ext;
                File dest = new File(newDir, savedName);
                sub.transferTo(dest);

                String dbPath = "category/" + categoryDirName + "/" + productDirName + "/" + savedName;
                images.add(new ProductImage(null, product, dbPath, null, seq));
                seq++;
            }
        }

        // 5️⃣ 새 상세 이미지 저장
        if (detailImages != null) {
            int seq = 1;
            for (MultipartFile detail : detailImages) {
                if (detail == null || detail.isEmpty()) continue;

                String ext = Optional.ofNullable(detail.getOriginalFilename())
                        .orElse("img").replaceAll(".*(\\.[^.]+)$", "$1");

                String savedName = seq + ext;
                File dest = new File(newDir, savedName);
                detail.transferTo(dest);

                String dbPath = "category/" + categoryDirName + "/" + productDirName + "/" + savedName;
                images.add(new ProductImage(null, product, dbPath, null, seq));
                seq++;
            }
        }

        // 이미지를 하나라도 추가했다면 새로 저장
        if (images.isEmpty())
            throw new IllegalArgumentException("상품 이미지는 최소 1개 이상 필요합니다.");

        product.setImages(images);
        product.setMainImage(mainImagePath);

        productRepository.save(product);

        productLogRepository.save(ProductLog.builder()
                .product(product)
                .member(admin)
                .event("UPDATE")
                .createdAt(LocalDateTime.now())
                .build());
    }


    // 상품 삭제
    public void deleteProduct(Long id, Long memberId) {
        Member admin = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + id));
        if (rentalItemRepository.existsByProduct(product))
            throw new IllegalStateException("주문이 들어온 상품은 삭제할 수 없습니다.");

        product.delete();
        productRepository.save(product);

        productLogRepository.save(ProductLog.builder()
                .product(product)
                .member(admin)
                .event("DELETE")
                .createdAt(LocalDateTime.now())
                .build());
    }
}