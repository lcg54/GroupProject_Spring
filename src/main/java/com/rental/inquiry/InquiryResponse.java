package com.rental.inquiry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rental.constant.InquiryType;
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
    private Long memberId;
    private String member; // 작성자 이름
    private InquiryType type;
    private LocalDateTime createdAt;

    @JsonProperty("isSecret") // Lombok의 setter이슈(setSecret) 때문에 이름 고정함
    private boolean isSecret;

    private InquiryCommentResponse adminComment; // 관리자 답글 (없을 수도 있음)
}