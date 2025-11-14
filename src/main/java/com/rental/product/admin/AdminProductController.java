package com.rental.product.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
public class AdminProductController {

    private final AdminProductService adminProductService;
    private final ProductLogRepository productLogRepository;

    // 상품 등록
    @PostMapping(value = "/register/{memberId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(
            @PathVariable Long memberId,
            @RequestPart(value = "mainImage", required = true) MultipartFile mainImage,
            @RequestPart(value = "subImages", required = false) List<MultipartFile> subImages,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages,
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam String brand,
            @RequestParam(required = false) String description,
            @RequestParam Integer price,
            @RequestParam(defaultValue = "true") Boolean available,
            @RequestParam Integer totalStock
    ) throws IOException {
        adminProductService.register(memberId, mainImage, subImages, detailImages, name, category, brand, description, price, available, totalStock);
        return ResponseEntity.ok("상품 등록 완료");
    }

    // 상품 수정
    @PutMapping(value = "/{id}/{memberId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProduct(
            @PathVariable Long memberId,
            @PathVariable Long id,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "subImages", required = false) List<MultipartFile> subImages,
            @RequestPart(value = "detailImages", required = false) List<MultipartFile> detailImages,
            @RequestParam String name,
            @RequestParam String category,
            @RequestParam String brand,
            @RequestParam(required = false) String description,
            @RequestParam Integer price,
            @RequestParam(defaultValue = "true") Boolean available,
            @RequestParam Integer totalStock,
            @RequestParam(name = "reservedStock", defaultValue = "0") Integer reservedStock,
            @RequestParam(name = "shippingStock", defaultValue = "0") Integer shippingStock,
            @RequestParam(name = "rentedStock", defaultValue = "0") Integer rentedStock,
            @RequestParam(name = "repairStock", defaultValue = "0") Integer repairStock,
            @RequestParam(name = "existingImages", required = false, defaultValue = "[]") String existingImages
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<String> existing = (existingImages == null || existingImages.isBlank()) ? Collections.emptyList() : mapper.readValue(existingImages, new TypeReference<>() {});
            List<MultipartFile> allImages = new ArrayList<>();
            if(mainImage != null) allImages.add(mainImage);
            if(subImages != null) allImages.addAll(subImages);
            if(detailImages != null) allImages.addAll(detailImages);

            adminProductService.updateProduct(memberId, id, allImages, name, category, brand, description, price, available, totalStock, reservedStock, shippingStock, rentedStock, repairStock, existing);
            return ResponseEntity.ok("상품 수정 완료");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "상품 수정 실패: " + e.getMessage()));
        }
    }

    // 상품 삭제
    @DeleteMapping("/{id}/{memberId}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, @PathVariable Long memberId) {
        try {
            adminProductService.deleteProduct(id, memberId);
            return ResponseEntity.ok(Map.of("message", "상품 삭제 완료"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "상품 삭제 중 오류가 발생했습니다."));
        }
    }

    // 등록 로그
    @GetMapping("/logs")
    public ResponseEntity<List<Map<String, Object>>> getCreateLogs(){
        List<ProductLog> logs = productLogRepository.findByEventOrderByCreatedAtDesc("CREATE");
        return ResponseEntity.ok(logForm(logs));
    }

    // 수정/삭제 로그
    @GetMapping("/logs/changes")
    public ResponseEntity<List<Map<String, Object>>> getChangeLogs() {
        List<ProductLog> logs = productLogRepository.findByEventInOrderByCreatedAtDesc(List.of("UPDATE", "DELETE"));
        return ResponseEntity.ok(logForm(logs));
    }

    // 로그용 내부 DTO
    private List<Map<String, Object>> logForm(List<ProductLog> logs) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProductLog log : logs) {
            Map<String, Object> logUpdate = new HashMap<>();
            logUpdate.put("productId", log.getProduct().getId());
            logUpdate.put("productName", log.getProduct().getName());
            String adminName = "알 수 없음";
            if (log.getMember() != null) {
                try {
                    adminName = log.getMember().getName();
                } catch (Exception e) {
                    adminName = "로딩 오류";
                }
            }
            logUpdate.put("adminName", adminName);
            logUpdate.put("createdAt", log.getCreatedAt());
            logUpdate.put("event", log.getEvent());
            result.add(logUpdate);
        }
        return result;
    }
}
