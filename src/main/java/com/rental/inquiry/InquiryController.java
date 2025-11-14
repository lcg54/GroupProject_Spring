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

    // 회원별 문의글 조회
    @GetMapping("/member/{memberId}/inquiry")
    public ResponseEntity<Page<InquiryResponse>> getMemberInquiries(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        Page<InquiryResponse> inquiries = inquiryService.getInquiriesByMemberId(memberId, page, size);
        return ResponseEntity.ok(inquiries);
    }

    // 문의글 수정
    @PutMapping("/product/inquiry/{inquiryId}")
    public ResponseEntity<?> updateInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long requesterId,
            @RequestBody InquiryRequest request
    ) {
        inquiryService.updateInquiry(inquiryId, requesterId, request.getTitle(), request.getContent());
        return ResponseEntity.ok("문의가 수정되었습니다.");
    }

    // 문의글 삭제
    @DeleteMapping("/product/inquiry/{inquiryId}")
    public ResponseEntity<?> deleteInquiry(
            @PathVariable Long inquiryId,
            @RequestParam Long requesterId
    ) {
        inquiryService.deleteInquiry(inquiryId, requesterId);
        return ResponseEntity.ok("문의가 삭제되었습니다.");
    }

    // 관리자 전체 문의 조회 (모든 상품의 문의)
    @GetMapping("/admin/inquiry")
    public ResponseEntity<Page<InquiryResponse>> getAllInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(required = false) Boolean answered  // 답변 완료 여부 필터
    ) {
        Sort.Direction direction = sort.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        return ResponseEntity.ok(inquiryService.getAllInquiries(pageable, answered));
    }
}