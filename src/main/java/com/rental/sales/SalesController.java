package com.rental.sales;

import com.rental.constant.Category;
import com.rental.rental.RentalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * SalesController
 *
 * /api/sales/calendar
 *  - date                : 하루 기준 응답 (해당 날짜의 '납부된 월납입 합계' 반환)
 *  - startDate & endDate : 범위 조회 (날짜별 리스트 반환)
 *  - year & month        : 특정 연/월의 달력형 리스트 반환 (해당 월의 각 날짜별 월납입 합계)
 *  - year                : 연간(해당 연도의 날짜별 월납입 합계 리스트) 조회
 *
 * /api/sales/range
 *  - startDate & endDate : 범위 합계 (합계 객체 반환)
 *
 * /api/sales/details
 *  - date [+ optional filter] : 그 날짜에 결제(월납입) 발생한 주문들의 상세(서버에서 item 수준으로 필터링 후 반환)
 */
@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    /**
     * 달력용 데이터 조회
     *
     * 우선순위:
     *  1) date 단일일 조회
     *  2) startDate & endDate 범위 조회
     *  3) year & month -> 해당 월의 캘린더 (각 날짜별 월납입 합계)
     *  4) year -> 해당 연도의 날짜별 월납입 합계 (getYearlySales)
     *  5) 그 외 -> 빈 리스트 반환
     */
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
            // 특정 날짜: 해당 날짜의 월납입 합계를 반환 (서비스 내부에서 월 캘린더에서 해당 날짜 필터링)
            sales = salesService.getDailySales(date, category, rentalPeriod);
        } else if (startDate != null && endDate != null) {
            // 날짜 범위: 범위 내의 날짜별 월납입 합계 리스트 반환
            sales = salesService.getSalesInRange(startDate, endDate, category, rentalPeriod);
        } else if (year != null && month != null) {
            // 연+월: 해당 월의 달력(각 날짜별 월납입 합계) 반환
            sales = salesService.getMonthlySales(year, month, category, rentalPeriod);
        } else if (year != null) {
            // 연간: 해당 연도의 날짜별 월납입 합계 (1~12월을 합쳐 반환)
            sales = salesService.getYearlySales(year, category, rentalPeriod);
        } else {
            sales = List.of();
        }

        return ResponseEntity.ok(sales);
    }

    /**
     * 기간 합계 (startDate ~ endDate 범위의 총합)
     * 반환: SalesResponse(date = "start ~ end", orderCount, productCount, totalSales)
     *
     * totalSales는 '해당 기간에 실제 결제된(월납입) 금액'의 합으로 계산됩니다.
     */
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

    /**
     * 특정 날짜 주문 상세 내역
     * - 서버에서 item-level 필터(카테고리, 대여기간)를 적용하여 반환합니다.
     * - 프론트는 반환된 각 RentalResponse의 items를 사용해 모달을 렌더링합니다.
     */
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