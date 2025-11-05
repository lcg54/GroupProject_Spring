package com.rental.inquiry;

import com.rental.constant.InquiryType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryRequest {
    private Long memberId;
    private String title;
    private String content;
    private InquiryType type;
    private Boolean isSecret = false;
}