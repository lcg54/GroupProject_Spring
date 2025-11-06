package com.rental.controller;

import com.rental.dto.ServiceDateDto;
import com.rental.dto.ServiceDateRequest;
import com.rental.entity.RentalItem;
import com.rental.entity.ServiceDate;
import com.rental.repository.RentalItemRepository;
import com.rental.repository.ServiceDateRepository;
import com.rental.service.ServiceDateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/rental/service") // ✅ 절대 경로
@RequiredArgsConstructor
public class ServiceDateController {

    private final ServiceDateService serviceDateService;
    private final ServiceDateRepository serviceDateRepository;

    @GetMapping("/{rentalId}/service-dates")
    public List<ServiceDateDto> getServiceDates(@PathVariable Long rentalId) {
        List<ServiceDate> serviceDates = serviceDateRepository.findByRentalItem_RentalId(rentalId);
        return serviceDates.stream()
                .map(sd -> new ServiceDateDto(sd.getId(), sd.getServiceDate()))
                .toList();
    }

    @PostMapping("/add")
    public ResponseEntity<?> addService(@RequestBody ServiceDateRequest dto) {
        // 여기서 서비스 추가 로직
        serviceDateService.addServiceDate(dto.getRentalItemId(), dto.getServiceDate());
        return ResponseEntity.ok("등록 성공");
    }

    // DELETE 대신 POST로 삭제 처리 권장
    @PostMapping("/remove")
    public ResponseEntity<?> removeServiceDate(@RequestBody ServiceDateRequest dto) {
        serviceDateService.removeServiceDate(dto.getRentalItemId(), dto.getServiceDate());
        return ResponseEntity.ok("서비스 날짜 삭제 완료");
    }
}
