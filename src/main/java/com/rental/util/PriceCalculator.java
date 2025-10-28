package com.rental.util;

import org.springframework.stereotype.Component;

@Component
public class PriceCalculator {

    // 월 납부액
    public int calculateMonthlyPrice(int productPrice, int periodYears) {
        return productPrice / (periodYears * 20) - 5100;
    }

    // 총 납부액
    public int calculateTotalPrice(int monthlyPrice, int periodYears, int quantity) {
        return monthlyPrice * 12 * periodYears * quantity;
    }
}