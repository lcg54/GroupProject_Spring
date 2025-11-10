package com.rental.inquiry;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryCommentRequest {
    private Long adminId;
    private String comment;
}
