package com.rental.sales;

import com.rental.constant.Category;
import com.rental.rental.*;
import com.rental.rental.RentalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;

    // ---------- Helper: 해당 item이 특정 날짜(date)에 월납부가 발생하는지 판정 ----------
    private boolean isPaymentDueOn(RentalItem item, LocalDate date) {
        if (item == null || item.getRentalStart() == null || item.getRentalEnd() == null) return false;

        LocalDate start = item.getRentalStart();
        LocalDate end = item.getRentalEnd();

        // 날짜 범위에 포함되어야 함
        if (date.isBefore(start) || date.isAfter(end)) return false;

        int desiredDay = start.getDayOfMonth();
        int lastDayOfMonth = date.lengthOfMonth();

        // 만약 시작일의 day가 이번 달에 존재하지 않으면(예: 31일), 그 달의 마지막 날을 결제일로 본다.
        int effectiveDay = Math.min(desiredDay, lastDayOfMonth);

        return date.getDayOfMonth() == effectiveDay;
    }

    // ---------- Daily: 특정 날짜의 월납입 합계(카테고리/대여기간 필터 포함) ----------
    @Transactional(readOnly = true)
    public List<SalesResponse> getDailySales(LocalDate date, Category category, Integer rentalPeriod) {
        // 수월성을 위해 rentalItemRepository 에서 date 범위(이날 포함)와 필터로 겹치는 items 를 가져온다.
        List<RentalItem> items = rentalItemRepository.findItemsOverlappingRangeWithFilters(date, date, category, rentalPeriod);

        // map: date -> aggregate (here only single date)
        int orderCount = 0;
        int productCount = 0;
        int totalSales = 0;

        // orderCount는 '같은 rental'이 하나라도 결제 항목을 가지면 1건으로 셈
        Set<Long> rentalsWithPayment = new HashSet<>();

        for (RentalItem item : items) {
            if (isPaymentDueOn(item, date)) {
                rentalsWithPayment.add(item.getRental().getId());
                int qty = item.getQuantity();
                int monthlyPerUnit = item.getMonthlyPrice();
                productCount += qty;
                totalSales += monthlyPerUnit * qty;
            }
        }
        orderCount = rentalsWithPayment.size();

        SalesResponse resp = new SalesResponse(date.toString(), orderCount, productCount, totalSales);
        return List.of(resp);
    }

    // ---------- Range: startDate ~ endDate 사이 날짜별 월납입 합계 리스트 ----------
    @Transactional(readOnly = true)
    public List<SalesResponse> getSalesInRange(LocalDate startDate, LocalDate endDate, Category category, Integer rentalPeriod) {
        if (endDate.isBefore(startDate)) return List.of();

        // 1) 해당 범위와 겹치는 모든 RentalItem을 한 번에 조회 (DB 호출 최소화)
        List<RentalItem> items = rentalItemRepository.findItemsOverlappingRangeWithFilters(startDate, endDate, category, rentalPeriod);

        List<SalesResponse> results = new ArrayList<>();

        // 2) 각 날짜별로 집계
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            int orderCount = 0;
            int productCount = 0;
            int totalSales = 0;

            Set<Long> rentalsWithPayment = new HashSet<>();

            for (RentalItem item : items) {
                if (isPaymentDueOn(item, cursor)) {
                    rentalsWithPayment.add(item.getRental().getId());
                    int qty = item.getQuantity();
                    int monthlyPerUnit = item.getMonthlyPrice();
                    productCount += qty;
                    totalSales += monthlyPerUnit * qty;
                }
            }

            orderCount = rentalsWithPayment.size();
            results.add(new SalesResponse(cursor.toString(), orderCount, productCount, totalSales));
            cursor = cursor.plusDays(1);
        }

        // 정렬: 날짜 오름차순
        results.sort(Comparator.comparing(SalesResponse::getDate));
        return results;
    }

    // ---------- Monthly: 특정 연/월의 달력(해당 월의 각 날짜별 월납입 합계) ----------
    @Transactional(readOnly = true)
    public List<SalesResponse> getMonthlySales(int year, int month, Category category, Integer rentalPeriod) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return getSalesInRange(start, end, category, rentalPeriod);
    }

    // ---------- Yearly: 해당 연도의 날짜별 월납입 합계(1월1일~12월31일) ----------
    @Transactional(readOnly = true)
    public List<SalesResponse> getYearlySales(int year, Category category, Integer rentalPeriod) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        return getSalesInRange(start, end, category, rentalPeriod);
    }

    // ---------- Total in Range: 합계 객체 (start ~ end 기간 내 실제 결제된 월납입 합계) ----------
    @Transactional(readOnly = true)
    public SalesResponse getSalesTotalInRange(LocalDate startDate, LocalDate endDate, Category category, Integer rentalPeriod) {
        List<SalesResponse> list = getSalesInRange(startDate, endDate, category, rentalPeriod);

        int orderCount = list.stream().mapToInt(SalesResponse::getOrderCount).sum(); // 날짜별 주문 수 합 (필요시 다른 방식 변경)
        int productCount = list.stream().mapToInt(SalesResponse::getProductCount).sum();
        int totalSales = list.stream().mapToInt(SalesResponse::getTotalSales).sum();

        String label = startDate.toString() + " ~ " + endDate.toString();
        return new SalesResponse(label, orderCount, productCount, totalSales);
    }

    // ---------- Daily Details: 그 날짜에 실제 결제(월납입) 발생한 주문들(서버에서 item-level 필터 적용) ----------
    @Transactional(readOnly = true)
    public List<RentalResponse> getDailySalesDetails(LocalDate date, Category category, Integer rentalPeriod) {
        // 1) 해당 날짜와 겹치는 items (필터 포함) 조회
        List<RentalItem> items = rentalItemRepository.findItemsOverlappingRangeWithFilters(date, date, category, rentalPeriod);

        // 2) items 중 그 날짜에 월납입이 발생하는 항목만 남기고, rental 단위로 그룹화
        Map<Long, List<RentalItem>> groupedByRental = items.stream()
                .filter(item -> isPaymentDueOn(item, date))
                .collect(Collectors.groupingBy(item -> item.getRental().getId()));

        // 3) 각 rental로 DTO 변환 (itemTotalPrice 는 월납입액)
        List<RentalResponse> responses = new ArrayList<>();
        for (Map.Entry<Long, List<RentalItem>> entry : groupedByRental.entrySet()) {
            Long rentalId = entry.getKey();
            List<RentalItem> payingItems = entry.getValue();

            Rental rental = payingItems.get(0).getRental(); // 같은 rental 객체 공유됨

            List<RentalResponse.RentalItemResponse> itemResponses = payingItems.stream()
                    .map(it -> {
                        int pricePerUnit = it.getMonthlyPrice();
                        int qty = it.getQuantity();
                        int monthly = pricePerUnit * qty; // 월납입액
                        return new RentalResponse.RentalItemResponse(
                                it.getId(),
                                it.getProduct() != null ? it.getProduct().getId() : null,
                                it.getProduct() != null ? it.getProduct().getName() : null,
                                qty,
                                pricePerUnit,
                                it.getRentalPeriodYears(),
                                it.getRentalStart(),
                                it.getRentalEnd(),
                                monthly,
                                it.getStatus(),
                                it.getPaymentStatus(),
                                it.getProduct().getMainImage()
                        );
                    })
                    .collect(Collectors.toList());

            int totalPrice = itemResponses.stream().mapToInt(RentalResponse.RentalItemResponse::getItemTotalPrice).sum();

            RentalResponse rr = new RentalResponse(
                    rental.getId(),
                    rental.getCreatedAt(),
                    totalPrice,
                    itemResponses
            );

            responses.add(rr);
        }

        // 정렬: 최신 주문 먼저(원하면 변경)
        responses.sort(Comparator.comparing(RentalResponse::getCreatedAt).reversed());
        return responses;
    }
}