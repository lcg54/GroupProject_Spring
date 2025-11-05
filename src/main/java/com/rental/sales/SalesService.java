package com.rental.sales;

import com.rental.constant.Category;
import com.rental.rental.RentalService;
import com.rental.review.RentalResponse;
import com.rental.rental.Rental;
import com.rental.rental.RentalItem;
import com.rental.rental.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final RentalRepository rentalRepository;
    private final RentalService rentalService;

    @Transactional(readOnly = true)
    public List<SalesResponse> getDailySales(LocalDate date, Category category, Integer rentalPeriod) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Rental> rentals = rentalRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        rentals = filterRentals(rentals, category, rentalPeriod);

        if (rentals.isEmpty()) {
            return List.of();
        }

        return List.of(calculateSales(date.toString(), rentals));
    }

    @Transactional(readOnly = true)
    public List<SalesResponse> getSalesInRange(LocalDate startDate, LocalDate endDate, Category category, Integer rentalPeriod) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Rental> rentals = rentalRepository.findByCreatedAtBetween(start, end);
        rentals = filterRentals(rentals, category, rentalPeriod);

        // 날짜별 그룹화
        Map<LocalDate, List<Rental>> grouped = rentals.stream()
                .collect(Collectors.groupingBy(r -> r.getCreatedAt().toLocalDate()));

        List<SalesResponse> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Rental>> entry : grouped.entrySet()) {
            result.add(calculateSales(entry.getKey().toString(), entry.getValue()));
        }

        result.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<SalesResponse> getMonthlySales(int year, int month, Category category, Integer rentalPeriod) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        return getSalesInRange(startDate, endDate, category, rentalPeriod);
    }

    @Transactional(readOnly = true)
    public List<SalesResponse> getYearlySales(int year, Category category, Integer rentalPeriod) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        return getSalesInRange(startDate, endDate, category, rentalPeriod);
    }

    @Transactional(readOnly = true)
    public SalesResponse getSalesTotalInRange(LocalDate startDate, LocalDate endDate, Category category, Integer rentalPeriod) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);

        List<Rental> rentals = rentalRepository.findByCreatedAtBetween(start, end);
        rentals = filterRentals(rentals, category, rentalPeriod);

        return calculateSales(startDate + " ~ " + endDate, rentals);
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getDailySalesDetails(LocalDate date, Category category, Integer rentalPeriod) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Rental> rentals = rentalRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        rentals = filterRentals(rentals, category, rentalPeriod);

        return rentals.stream()
                .map(rentalService::convertToResponse)
                .collect(Collectors.toList());
    }

    private List<Rental> filterRentals(List<Rental> rentals, Category category, Integer rentalPeriod) {
        if (category != null) {
            rentals = rentals.stream()
                    .filter(rental -> rental.getItems().stream()
                            .anyMatch(item -> item.getProduct().getCategory() == category))
                    .collect(Collectors.toList());
        }

        if (rentalPeriod != null) {
            rentals = rentals.stream()
                    .filter(rental -> rental.getItems().stream()
                            .anyMatch(item -> item.getRentalPeriodYears() == rentalPeriod))
                    .collect(Collectors.toList());
        }

        return rentals;
    }

    private SalesResponse calculateSales(String date, List<Rental> rentals) {
        int orderCount = rentals.size();

        int productCount = rentals.stream()
                .flatMap(rental -> rental.getItems().stream())
                .mapToInt(RentalItem::getQuantity)
                .sum();

        int totalSales = rentals.stream()
                .mapToInt(Rental::getTotalPrice)
                .sum();

        return new SalesResponse(date, orderCount, productCount, totalSales);
    }
}