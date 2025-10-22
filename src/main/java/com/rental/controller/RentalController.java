package com.rental.controller;

import com.rental.dto.RentalRequest;
import com.rental.dto.RentalResponse;
import com.rental.entity.Rental;
import com.rental.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;

    @PostMapping
    public ResponseEntity<RentalResponse> createRental(@RequestBody RentalRequest request) {
        Rental rental = rentalService.createRental(request);
        RentalResponse response = rentalService.convertToResponse(rental);
        return ResponseEntity.ok(response);
    }
}