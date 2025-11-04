package com.rental.controller;

import com.rental.constant.Category;
import com.rental.dto.RentalResponse;
import com.rental.dto.SalesResponse;
import com.rental.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class SalesController {

    private final SalesService salesService;

    // 달력용 데이터
    @GetMapping("/calendar")
    public ResponseEntity<List<SalesResponse>> getSalesCalendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Integer rentalPeriod
    ) {
        List<SalesResponse> sales;

        if (date != null) {
            sales = salesService.getDailySales(date, category, rentalPeriod);
        } else if (startDate != null && endDate != null) {
            sales = salesService.getSalesInRange(startDate, endDate, category, rentalPeriod);
        } else if (year != null && month != null) {
            sales = salesService.getMonthlySales(year, month, category, rentalPeriod);
        } else if (year != null) {
            sales = salesService.getYearlySales(year, category, rentalPeriod);
        } else {
            sales = List.of();
        }

        return ResponseEntity.ok(sales);
    }

    // 기간별 합계
    @GetMapping("/range")
    public ResponseEntity<SalesResponse> getSalesRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Integer rentalPeriod
    ) {
        SalesResponse sales = salesService.getSalesTotalInRange(startDate, endDate, category, rentalPeriod);
        return ResponseEntity.ok(sales);
    }

    // 특정 날짜 주문 상세 내역
    @GetMapping("/details")
    public ResponseEntity<List<RentalResponse>> getSalesDetails(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Integer rentalPeriod
    ) {
        List<RentalResponse> details = salesService.getDailySalesDetails(date, category, rentalPeriod);
        return ResponseEntity.ok(details);
    }
}