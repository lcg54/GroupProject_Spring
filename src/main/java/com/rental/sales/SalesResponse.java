package com.rental.sales;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SalesResponse {
    private String date;
    private int orderCount;
    private int productCount;
    private int totalSales;
}