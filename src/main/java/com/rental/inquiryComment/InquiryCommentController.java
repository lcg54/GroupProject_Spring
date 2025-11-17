package com.rental.inquiryComment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class InquiryCommentController {
    private final InquiryCommentService inquiryCommentService;

    // 관리자 답변 등록
    @PostMapping("/product/{productId}/inquiry/{inquiryId}/comment")
    public ResponseEntity<?> createAdminComment(
            @PathVariable Long productId,
            @PathVariable Long inquiryId,
            @RequestBody InquiryCommentRequest inquiryCommentRequest
    ) {
        var saved = inquiryCommentService.createAdminComment(
                inquiryId,
                inquiryCommentRequest.getAdminId(),
                inquiryCommentRequest.getComment()
        );
        return ResponseEntity.ok(saved);
    }

    // 관리자 답변 수정
    @PutMapping("/product/{productId}/inquiry/{inquiryId}/comment")
    public ResponseEntity<?> updateAdminComment(
            @PathVariable Long productId,
            @PathVariable Long inquiryId,
            @RequestParam Long requesterId,
            @RequestBody InquiryCommentRequest request
    ) {
        inquiryCommentService.updateAdminComment(inquiryId, requesterId, request.getComment());
        return ResponseEntity.ok("답변이 수정되었습니다.");
    }

    // 관리자 답변 삭제
    @DeleteMapping("/product/{productId}/inquiry/{inquiryId}/comment")
    public ResponseEntity<?> deleteAdminComment(
            @PathVariable Long productId,
            @PathVariable Long inquiryId,
            @RequestParam Long requesterId
    ) {
        inquiryCommentService.deleteAdminComment(inquiryId, requesterId);
        return ResponseEntity.ok("답변이 삭제되었습니다.");
    }
}