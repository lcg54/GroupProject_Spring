package com.rental.controller;

import com.rental.constant.RentalStatus;
import com.rental.dto.RentalRequest;
import com.rental.dto.RentalResponse;
import com.rental.entity.Rental;
import com.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;

    @PostMapping("/{id}")
    public ResponseEntity<RentalResponse> createRental(@RequestBody RentalRequest request) {
        Rental rental = rentalService.createRental(request.getMemberId(), request.getProductId(), request.getPeriodYears());
        RentalResponse response = new RentalResponse(
            rental.getId(),
            rental.getCreatedAt(),
            rental.getStatus(),
            rental.getRentalStart(),
            rental.getRentalEnd(),
            rental.getRentalPeriodYears(),
            rental.getMonthlyPrice(),
            rental.getTotalPrice()
        );
        return ResponseEntity.ok(response);
    }
}
