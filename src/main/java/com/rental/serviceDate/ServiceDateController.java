package com.rental.serviceDate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/rental/service")
@RequiredArgsConstructor
public class ServiceDateController {

    private final ServiceDateService serviceDateService;
    private final ServiceDateRepository serviceDateRepository;

    @GetMapping("/{rentalItemId}/service-dates")
    public List<ServiceDateDto> getServiceDates(@PathVariable Long rentalItemId) {
        // rentalItem 기준으로 ServiceDate 조회
        List<ServiceDate> serviceDates = serviceDateRepository.findByRentalItem_Id(rentalItemId);

        return serviceDates.stream()
                .map(sd -> new ServiceDateDto(sd.getId(), sd.getServiceDate()))
                .toList();
    }

    @PostMapping("/add")
    public ResponseEntity<?> addService(@RequestBody ServiceDateRequest dto) {
        try {
            // 문자열 "YYYY-MM-DD" → LocalDate
            LocalDate date = dto.getServiceDate();
            // 필요 시 명시적 변환 (LocalDate.parse)
            serviceDateService.addServiceDate(dto.getRentalId(), date);
            return ResponseEntity.ok("등록 성공");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeServiceDate(@RequestBody ServiceDateRequest dto) {
        serviceDateService.removeServiceDate(dto.getRentalItemId(), dto.getServiceDate());
        return ResponseEntity.ok("서비스 날짜 삭제 완료");
    }
}