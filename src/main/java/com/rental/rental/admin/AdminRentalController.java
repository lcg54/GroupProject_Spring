package com.rental.rental.admin;

import com.rental.constant.RentalStatus;
import com.rental.rental.RentalResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class AdminRentalController {
    private final AdminRentalService adminRentalService;

    // 관리자 - 전체 대여 조회
    @GetMapping("/control")
    public ResponseEntity<Map<String, Object>> getRentalsByStatus(
            @RequestParam(required = false, defaultValue = "RESERVED") RentalStatus status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "5") int size
    ) {
        Page<RentalResponse.RentalItemResponse> rentalPage = adminRentalService.getRentalItemsByStatus(status, page, size);
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
        String result = adminRentalService.updateRentalItemStatus(itemId, request.getNewStatus());
        return ResponseEntity.ok(result);
    }

    @Data
    public static class RentalStatusRequest {
        private RentalStatus newStatus;
    }

    // 관리자 - 탭별 총 아이템 수 조회
    @GetMapping("/control/count")
    public ResponseEntity<TotalCountResponse> getTotalCountByStatus(@RequestParam RentalStatus status) {
        long count = adminRentalService.countItemsByStatus(status);
        return ResponseEntity.ok(new TotalCountResponse(count));
    }

    @Data
    public static class TotalCountResponse {
        private final long totalItems;
    }
}
