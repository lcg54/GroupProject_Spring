package com.rental.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryCommentResponse {
    private String admin; // 관리자 이름
    private String comment;
    private LocalDateTime createdAt;
}