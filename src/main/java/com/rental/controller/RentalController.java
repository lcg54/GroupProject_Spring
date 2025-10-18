package com.rental.controller;

import com.rental.dto.RentalRequest;
import com.rental.entity.Rental;
import com.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;

    @PostMapping("/{id}")
    public ResponseEntity<Rental> createRental(@RequestBody RentalRequest request) {
        Rental rental = rentalService.createRental(request.getMemberId(), request.getProductId(), request.getPeriodYears());
        return ResponseEntity.ok(rental);
    }
}
