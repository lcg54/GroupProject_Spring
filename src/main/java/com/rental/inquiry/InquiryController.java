package com.rental.inquiry;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class InquiryController {
    private final InquiryService inquiryService;

    // 상품별 문의글 조회
    @GetMapping("/product/{productId}/inquiry")
    public ResponseEntity<Page<InquiryResponse>> getProductInquiries(
            @PathVariable Long productId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "desc") String sort
    ) {
        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        return ResponseEntity.ok(inquiryService.getInquiriesByProduct(productId, memberId, pageable));
    }

    // 문의글 생성
    @PostMapping("/product/{productId}/inquiry/write")
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
    @PostMapping("/product/{productId}/inquiry/{inquiryId}/comment")
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

    // 회원별 문의글 조회
    @GetMapping("/member/{memberId}/inquiry")
    public ResponseEntity<Page<InquiryResponse>> getMemberInquiries(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Page<InquiryResponse> inquiries = inquiryService.getInquiriesByMemberId(memberId, page, size);
        return ResponseEntity.ok(inquiries);
    }

}