package com.rental.serviceDate;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rental/service")
@RequiredArgsConstructor
public class ServiceDateController {

    private final ServiceDateService serviceDateService;
    private final ServiceDateRepository serviceDateRepository;

    @GetMapping("/{rentalItemId}/service-dates")
    public List<ServiceDateDto> getServiceDates(@PathVariable Long rentalItemId) {
        List<ServiceDate> serviceDates = serviceDateRepository.findByRentalItem_Id(rentalItemId);

        return serviceDates.stream()
                .map(sd -> new ServiceDateDto(sd.getRentalItem().getId(), sd.getServiceDate()))
                .toList();
    }

    @PostMapping("/add")
    public ResponseEntity<String> addService(@RequestBody ServiceDateRequest dto) {
        try {
            serviceDateService.addServiceDate(dto.getRentalId(), dto.getRentalItemId(), dto.getServiceDate());
            return ResponseEntity.ok("서비스 날짜가 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<String> removeServiceDate(@RequestBody ServiceDateRequest dto) {
        try {
            boolean removed = serviceDateService.removeServiceDate(dto.getRentalItemId(), dto.getServiceDate());
            if (removed) {
                return ResponseEntity.ok("서비스 날짜가 삭제되었습니다.");
            } else {
                return ResponseEntity.badRequest().body("해당 서비스 날짜를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}