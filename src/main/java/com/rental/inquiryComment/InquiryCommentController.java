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
}
