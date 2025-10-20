package com.rental.controller;

import com.rental.constant.InquiryType;
import com.rental.entity.Inquiry;
import com.rental.entity.InquiryComment;
import com.rental.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inquiry")
public class InquiryController {

    private final InquiryService inquiryService;

    // 상품별 문의글 조회
    @GetMapping("/{productId}")
    public ResponseEntity<List<Inquiry>> getInquiriesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(inquiryService.getInquiriesByProduct(productId));
    }

    // 문의글 등록
    @PostMapping
    public ResponseEntity<?> createInquiry(@RequestBody Map<String, Object> req) {
        try {
            Long memberId = ((Number) req.get("memberId")).longValue();
            Long productId = ((Number) req.get("productId")).longValue();
            String title = (String) req.get("title");
            String content = (String) req.get("content");
            InquiryType type = InquiryType.valueOf((String) req.get("type"));

            Inquiry saved = inquiryService.createInquiry(memberId, productId, title, content, type);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 관리자 답글 등록
    @PostMapping("/{inquiryId}/comment")
    public ResponseEntity<?> createAdminComment(
            @PathVariable Long inquiryId,
            @RequestBody Map<String, Object> req
    ) {
        try {
            Long adminId = ((Number) req.get("adminId")).longValue();
            String comment = (String) req.get("comment");

            InquiryComment saved = inquiryService.createAdminComment(adminId, inquiryId, comment);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}