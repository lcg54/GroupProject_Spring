package com.rental.controller;

import com.rental.dto.InquiryCommentRequest;
import com.rental.dto.InquiryRequest;
import com.rental.dto.InquiryResponse;
import com.rental.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product/{productId}/inquiry")
public class InquiryController {
    private final InquiryService inquiryService;

    // 상품별 문의글 조회
    @GetMapping
    public ResponseEntity<Page<InquiryResponse>> getInquiriesByProduct(
            @PathVariable Long productId,
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "desc") String sort
    ) {
        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        return ResponseEntity.ok(inquiryService.getInquiriesByProduct(productId, memberId, pageable));
    }

    // 문의글 생성
    @PostMapping("/write")
    public ResponseEntity<?> createInquiry(
            @PathVariable Long productId,
            @RequestBody InquiryRequest inquiryRequest
    ) {
        var saved = inquiryService.createInquiry(
                inquiryRequest.getMemberId(),
                productId,
                inquiryRequest.getTitle(),
                inquiryRequest.getContent(),
                inquiryRequest.getType(),
                inquiryRequest.getIsSecret()
        );
        return ResponseEntity.ok(saved);
    }

    // 관리자 답변 등록
    @PostMapping("/{inquiryId}/comment")
    public ResponseEntity<?> createAdminComment(
            @PathVariable Long productId,
            @PathVariable Long inquiryId,
            @RequestBody InquiryCommentRequest inquiryCommentRequest
    ) {
        var saved = inquiryService.createAdminComment(
                inquiryId,
                inquiryCommentRequest.getAdminId(),
                inquiryCommentRequest.getComment()
        );
        return ResponseEntity.ok(saved);
    }
}