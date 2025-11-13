package com.rental.rental.admin;

import com.rental.constant.RentalStatus;
import com.rental.product.Product;
import com.rental.product.ProductRepository;
import com.rental.rental.RentalItem;
import com.rental.rental.RentalItemRepository;
import com.rental.rental.RentalResponse;
import com.rental.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rental")
@RequiredArgsConstructor
public class AdminRentalService {
    private final RentalItemRepository rentalItemRepository;
    private final ProductRepository productRepository;
    private final PriceCalculator calculator;

    // 관리자 전용 조회
    @Transactional(readOnly = true)
    public Page<RentalResponse.RentalItemResponse> getRentalItemsByStatus(RentalStatus status, int page, int size) {
        Sort sort;
        switch (status) {
            case RESERVED, SHIPPING -> sort = Sort.by(Sort.Direction.ASC, "rentalStart"); // 예약일(미래) 가까운 순
            case RENTED, RETURN_REQUESTED -> sort = Sort.by(Sort.Direction.ASC, "rentalEnd"); // 종료일(미래) 가까운 순
            case RETURNED, CANCELED -> sort = Sort.by(Sort.Direction.DESC, "rentalEnd");  // 종료일(과거) 가까운 순
            default -> sort = Sort.by(Sort.Direction.ASC, "rentalStart");
        }

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<RentalItem> rentalItemsPage = rentalItemRepository.findByStatus(status, pageable);

        List<RentalResponse.RentalItemResponse> itemResponses = rentalItemsPage.getContent().stream()
                .map(item -> new RentalResponse.RentalItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getMonthlyPrice(),
                        item.getRentalPeriodYears(),
                        item.getRentalStart(),
                        item.getRentalEnd(),
                        calculator.calculateTotalPrice(item.getMonthlyPrice(), item.getRentalPeriodYears(), item.getQuantity()),
                        item.getStatus(),
                        item.getPaymentStatus(),
                        item.getProduct().getMainImage()
                )).toList();

        return new PageImpl<>(itemResponses, pageable, rentalItemsPage.getTotalElements());
    }

    // 상태 변경 및 재고 반영
    @Transactional
    public String updateRentalItemStatus(Long itemId, RentalStatus newStatus) {
        RentalItem item = rentalItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 대여 상품을 찾을 수 없습니다."));

        Product product = item.getProduct();
        RentalStatus oldStatus = item.getStatus();

        // 상태가 바뀔 때마다 재고 동기화
        adjustProductStock(product, oldStatus, newStatus, item.getQuantity());

        // 상태 업데이트
        item.setStatus(newStatus);
        rentalItemRepository.save(item);

        return "상품 상태가 '" + newStatus + "'로 변경되었습니다.";
    }

    // 재고 조정 로직
    private void adjustProductStock(Product product, RentalStatus oldStatus, RentalStatus newStatus, int quantity) {
        // 이전 상태 재고 감소
        switch (oldStatus) {
            case RESERVED -> product.setReservedStock(Math.max(product.getReservedStock() - quantity, 0));
            case SHIPPING -> product.setShippingStock(Math.max(product.getShippingStock() - quantity, 0));
            case RENTED -> product.setRentedStock(Math.max(product.getRentedStock() - quantity, 0));
            case REPAIR -> product.setRepairStock(Math.max(product.getRepairStock() - quantity, 0));
            case RETURN_REQUESTED -> product.setReturnRequestedStock(Math.max(product.getReturnRequestedStock() - quantity, 0));
            default -> {}
        }
        // 새 상태 재고 증가
        switch (newStatus) {
            case RESERVED -> product.setReservedStock(product.getReservedStock() + quantity);
            case SHIPPING -> product.setShippingStock(product.getShippingStock() + quantity);
            case RENTED -> product.setRentedStock(product.getRentedStock() + quantity);
            case REPAIR -> product.setRepairStock(product.getRepairStock() + quantity);
            case RETURN_REQUESTED -> product.setReturnRequestedStock(product.getReturnRequestedStock() + quantity);
            case RETURNED, CANCELED -> {} // 아무 처리 없음 (대여 가능 재고로 돌아감)
            default -> {}
        }

        // 사용 가능 여부 자동 반영
        product.setAvailable(product.getAvailableStock() > 0);
        productRepository.save(product);
    }

    // 상태별 총 아이템 수
    @Transactional(readOnly = true)
    public long countItemsByStatus(RentalStatus status) {
        return rentalItemRepository.countByStatus(status);
    }
}
