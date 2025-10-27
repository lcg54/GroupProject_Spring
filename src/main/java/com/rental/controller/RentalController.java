package com.rental.controller;

import com.rental.constant.RentalStatus;
import com.rental.dto.RentalRequest;
import com.rental.dto.RentalResponse;
import com.rental.entity.Rental;
import com.rental.service.RentalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;

    // 대여 주문 생성
    @PostMapping
    public ResponseEntity<RentalResponse> createRental(@RequestBody RentalRequest request) {
        Rental rental = rentalService.createRental(request);
        RentalResponse response = rentalService.convertToResponse(rental);
        return ResponseEntity.ok(response);
    }

    // 관리자 - 전체 대여 조회
    @GetMapping("/control")
    public ResponseEntity<Map<String, Object>> getRentalsByStatus(
            @RequestParam(required = false, defaultValue = "RESERVED") RentalStatus status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size
    ) {
        Page<RentalResponse.RentalItemResponse> rentalPage = rentalService.getRentalItemsByStatus(status, page, size);
        return ResponseEntity.ok(Map.of(
                "items", rentalPage.getContent(),
                "totalPages", rentalPage.getTotalPages(),
                "totalItems", rentalPage.getTotalElements()
        ));
    }

    // 관리자 - 대여 상태 변경
    @PatchMapping("/control/status/{itemId}")
    public ResponseEntity<String> updateRentalItemStatus(
            @PathVariable Long itemId,
            @RequestBody RentalStatusRequest request
    ) {
        String result = rentalService.updateRentalItemStatus(itemId, request.getNewStatus());
        return ResponseEntity.ok(result);
    }

    // DTO인데 하나짜리라 걍 내부에 만듦
    @Data
    public static class RentalStatusRequest {
        private RentalStatus newStatus;
    }

    // 관리자 - 탭별 총 아이템 수 조회
    @GetMapping("/control/count")
    public ResponseEntity<TotalCountResponse> getTotalCountByStatus(@RequestParam RentalStatus status) {
        long count = rentalService.countItemsByStatus(status);
        return ResponseEntity.ok(new TotalCountResponse(count));
    }

    @Data
    public static class TotalCountResponse {
        private final long totalItems;
    }
}