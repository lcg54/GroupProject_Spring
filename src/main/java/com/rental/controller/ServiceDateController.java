package com.rental.controller;

import com.rental.entity.RentalItem;
import com.rental.entity.ServiceDate;
import com.rental.repository.RentalItemRepository;
import com.rental.service.ServiceDateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/service")
public class ServiceDateController {

    private final ServiceDateService serviceDateService;

    public ServiceDateController(ServiceDateService serviceDateService) {
        this.serviceDateService = serviceDateService;
    }

    // 서비스 날짜 조회
    @GetMapping
    public List<LocalDate> getServiceDates(@RequestParam Long rentalItemId) {
        return serviceDateService.getServiceDatesByRentalId(rentalItemId);
    }

    // 서비스 날짜 추가
    @PostMapping("/add")
    public ResponseEntity<ServiceDate> addServiceDate(
            @RequestParam Long rentalItemId,
            @RequestParam String serviceDate) {

        LocalDate date = LocalDate.parse(serviceDate);
        ServiceDate newServiceDate = serviceDateService.addServiceDate(rentalItemId, date);
        return ResponseEntity.ok(newServiceDate);
    }

    // 서비스 날짜 삭제
    @DeleteMapping("/remove")
    public ResponseEntity<String> removeServiceDate(
            @RequestParam Long rentalItemId,
            @RequestParam String serviceDate) {

        LocalDate date = LocalDate.parse(serviceDate);
        boolean removed = serviceDateService.removeServiceDate(rentalItemId, date);

        if (removed) {
            return ResponseEntity.ok("서비스 날짜가 삭제되었습니다.");
        } else {
            return ResponseEntity.status(404).body("해당 날짜가 존재하지 않습니다.");
        }
    }
}

