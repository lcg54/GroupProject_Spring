package com.rental.payment;

import lombok.Data;

@Data
public class BillingRequest {
    private String customerKey; // 회원 고유 식별자
    private String authKey;     // 프론트에서 받은 authKey
}