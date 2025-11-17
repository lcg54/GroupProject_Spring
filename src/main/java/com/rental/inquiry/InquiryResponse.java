package com.rental.inquiry;

import com.rental.constant.InquiryType;
import com.rental.inquiryComment.InquiryCommentResponse;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryResponse {
    private Long id;
    private String title;
    private String content;
    private Long productId;
    private String productName;
    private Long memberId;
    private String member;
    private InquiryType type;
    private LocalDateTime createdAt;
    private boolean isSecret;
    private InquiryCommentResponse adminComment;
}