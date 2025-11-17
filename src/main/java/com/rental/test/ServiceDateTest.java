package com.rental.test;

import com.rental.rental.Rental;
import com.rental.rental.RentalItem;
import com.rental.rental.RentalItemRepository;
import com.rental.serviceDate.ServiceDate;
import com.rental.serviceDate.ServiceDateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@SpringBootTest
public class ServiceDateTest {

    @Autowired
    private RentalItemRepository rentalItemRepository;

    @Autowired
    private ServiceDateRepository serviceDateRepository;

    private static final Random random = new Random();

    @Test
    void insertSampleServiceDates() {

        List<RentalItem> rentalItems = rentalItemRepository.findAll();

        int totalItems = rentalItems.size();
        int createdDates = 0;
        int skipped = 0;

        List<ServiceDate> buffer = new ArrayList<>();

        for (RentalItem item : rentalItems) {
            LocalDate startDate = item.getRentalStart();
            LocalDate endDate = item.getRentalEnd();
            Rental rental = item.getRental();

            if (startDate == null || endDate == null) {
                skipped++;
                continue;
            }

            // 서비스 가능 시작일: 대여 시작 + 6개월
            LocalDate earliestServiceDate = startDate.plusMonths(6);

            if (earliestServiceDate.isAfter(endDate)) {
                skipped++;
                continue;
            }

            int startYear = earliestServiceDate.getYear();
            int endYear = endDate.getYear();

            Set<LocalDate> generatedForItem = new HashSet<>();
            int totalCapPerItem = 12;

            for (int year = startYear; year <= endYear; year++) {

                LocalDate yearStart = LocalDate.of(year, 1, 1);
                LocalDate yearEnd = LocalDate.of(year, 12, 31);

                LocalDate rangeStart = earliestServiceDate.isAfter(yearStart) ? earliestServiceDate : yearStart;
                LocalDate rangeEnd = endDate.isBefore(yearEnd) ? endDate : yearEnd;

                long dayRange = ChronoUnit.DAYS.between(rangeStart, rangeEnd) + 1;

                if (dayRange < 30) continue;

                int toCreateThisYear = random.nextInt(3);  // 0~2 회 생성

                if (generatedForItem.size() >= totalCapPerItem) break;

                for (int c = 0; c < toCreateThisYear; c++) {

                    LocalDate candidate = null;
                    int attempts = 0;

                    // 주말 제외 + 중복 제외를 만족할 때까지 뽑기
                    while (true) {
                        attempts++;
                        if (attempts > 50) break; // safety break

                        int offset = random.nextInt((int) dayRange);
                        candidate = rangeStart.plusDays(offset);

                        // 주말 제외 조건 적용!
                        DayOfWeek dow = candidate.getDayOfWeek();
                        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                            continue;
                        }

                        if (generatedForItem.contains(candidate)) {
                            continue;
                        }

                        break;
                    }

                    if (attempts > 50) continue;

                    // 생성
                    ServiceDate sd = new ServiceDate();
                    sd.setRentalItem(item);
                    sd.setServiceDate(candidate);
                    sd.setRental(rental);

                    buffer.add(sd);
                    generatedForItem.add(candidate);
                    createdDates++;

                    if (buffer.size() >= 1000) {
                        serviceDateRepository.saveAll(buffer);
                        buffer.clear();
                    }

                    if (generatedForItem.size() >= totalCapPerItem) break;
                }
            }

            if (generatedForItem.isEmpty()) {
                skipped++;
            }
        }

        if (!buffer.isEmpty()) {
            serviceDateRepository.saveAll(buffer);
            buffer.clear();
        }

        System.out.println("========== 서비스일 샘플 생성 완료 ==========");
        System.out.println("🎯 전체 RentalItem 수: " + totalItems);
        System.out.println("📅 생성된 서비스일 수: " + createdDates);
        System.out.println("⚠️ 스킵된 아이템 (서비스 불가 등): " + skipped);
    }
}
