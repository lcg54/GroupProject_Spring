package com.rental.payment.instant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PaymentConfirmResponse {
    private String status;
    private String tossResponse;
}