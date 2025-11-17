package com.rental.payment.fake;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FakeBillingRequest {
    private Long memberId;
    private String customerKey;
    private String cardNum;
}
