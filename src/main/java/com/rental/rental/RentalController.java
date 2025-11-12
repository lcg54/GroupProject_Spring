package com.rental.rental;

import com.rental.rental.find.FindRentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;
    private final FindRentalService findRentalService;

    // 대여 주문 생성 (=예약)
    @PostMapping
    public ResponseEntity<RentalResponse> createRental(@RequestBody RentalRequest request) {
        Rental rental = rentalService.createRental(request);
        RentalResponse response = findRentalService.convertToResponse(rental);
        return ResponseEntity.ok(response);
    }

    // 예약 취소
    @PostMapping("/delete/{rentalItemId}")
    public ResponseEntity<String> cancelRentalItem(@PathVariable Long rentalItemId) {
        String result = rentalService.cancelRentalItem(rentalItemId);
        return ResponseEntity.ok(result);
    }

    // 반납 요청
    @PostMapping("/requestReturn/{rentalItemId}")
    public ResponseEntity<String> requestReturn(@PathVariable Long rentalItemId) {
        String result = rentalService.requestReturn(rentalItemId);
        return ResponseEntity.ok(result);
    }

    // 반납 요청 취소
    @PostMapping("/cancelReturn/{rentalItemId}")
    public ResponseEntity<String> cancelReturn(@PathVariable Long rentalItemId) {
        return ResponseEntity.ok(rentalService.cancelReturnRequest(rentalItemId));
    }
}