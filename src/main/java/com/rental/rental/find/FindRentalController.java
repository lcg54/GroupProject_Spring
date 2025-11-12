package com.rental.rental.find;

import com.rental.rental.RentalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class FindRentalController {
    private final FindRentalService findRentalService;

    // 회원별 대여 내역 조회
    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<RentalResponse>> getRentalsByMember(@PathVariable Long memberId) {
        List<RentalResponse> rentals = findRentalService.getRentalsByMemberId(memberId);
        return ResponseEntity.ok(rentals);
    }

    // 회원별 리뷰를 쓰지 않은 대여 내역 조회
    @GetMapping("/member/{memberId}/unreviewed")
    public ResponseEntity<List<RentalResponse>> getUnreviewedRentals(@PathVariable Long memberId) {
        List<RentalResponse> rentals = findRentalService.getUnreviewedRentalsByMemberId(memberId);
        return ResponseEntity.ok(rentals);
    }

    // 특정 대여 상세 조회
    @GetMapping("/{rentalId}")
    public ResponseEntity<RentalResponse> getRentalDetail(@PathVariable Long rentalId) {
        RentalResponse rental = findRentalService.getRentalById(rentalId);
        return ResponseEntity.ok(rental);
    }
}
