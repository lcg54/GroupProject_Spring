package com.rental.service;

import com.rental.constant.RentalStatus;
import com.rental.dto.RentalRequest;
import com.rental.dto.RentalResponse;
import com.rental.entity.Member;
import com.rental.entity.Product;
import com.rental.entity.Rental;
import com.rental.entity.RentalItem;
import com.rental.repository.MemberRepository;
import com.rental.repository.ProductRepository;
import com.rental.repository.RentalItemRepository;
import com.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalService {
    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    // 대여 생성
    @Transactional
    public Rental createRental(RentalRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Rental rental = new Rental();
        rental.setMember(member);
        rentalRepository.save(rental);

        int totalPrice = 0;
        List<RentalItem> items = new ArrayList<>();

        for (RentalRequest.RentalItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

            if (product.getAvailableStock() < itemReq.getQuantity()) {
                throw new IllegalArgumentException("상품 재고가 부족합니다: " + product.getName());
            }

            // 대여료 계산 로직 (임시)
            int monthlyPrice = product.getPrice() / (itemReq.getPeriodYears() * 20) - 5100;
            int itemTotal = monthlyPrice * 12 * itemReq.getPeriodYears() * itemReq.getQuantity();

            RentalItem item = new RentalItem();
            item.setRental(rental);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setPricePerUnit(monthlyPrice);
            item.setRentalPeriodYears(itemReq.getPeriodYears());
            item.setRentalStart(itemReq.getRentalStart());
            item.setRentalEnd(itemReq.getRentalStart().plusYears(itemReq.getPeriodYears()));

            LocalDate today = LocalDate.now();
            int qty = itemReq.getQuantity();

            if (item.getRentalStart().isBefore(today)) { // 테스트 데이터 주입용 루트 (오늘 이전 주문은 대여중으로 처리)
                item.setStatus(RentalStatus.RENTED);
                product.setRentedStock(product.getRentedStock() + qty);
            } else { // 앱에서 신규 주문 시 동작할 루트 (오늘 또는 이후 주문은 예약으로 처리)
                item.setStatus(RentalStatus.RESERVED);
                product.setReservedStock(product.getReservedStock() + qty);
            }

            productRepository.save(product);

            items.add(item);
            totalPrice += itemTotal;
        }
        rental.setItems(items);
        rental.setTotalPrice(totalPrice);
        rentalItemRepository.saveAll(items);
        return rentalRepository.save(rental);
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
            default -> {}
        }
        // 새 상태 재고 증가
        switch (newStatus) {
            case RESERVED -> product.setReservedStock(product.getReservedStock() + quantity);
            case SHIPPING -> product.setShippingStock(product.getShippingStock() + quantity);
            case RENTED -> product.setRentedStock(product.getRentedStock() + quantity);
            case REPAIR -> product.setRepairStock(product.getRepairStock() + quantity);
            case RETURNED, CANCELED -> {
                // 아무 처리 없음 (대여 가능 재고로 돌아감)
            }
            default -> {}
        }

        // 사용 가능 여부 자동 반영
        product.setAvailable(product.getAvailableStock() > 0);
        productRepository.save(product);
    }

    // 관리자 전용 조회
    @Transactional(readOnly = true)
    public Page<RentalResponse.RentalItemResponse> getRentalItemsByStatus(RentalStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());
        Page<RentalItem> rentalItemsPage = rentalItemRepository.findByStatusOrderByRentalEndAsc(status, pageable);
        List<RentalResponse.RentalItemResponse> itemResponses = rentalItemsPage.getContent().stream()
                .map(item -> new RentalResponse.RentalItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPricePerUnit(),
                        item.getRentalPeriodYears(),
                        item.getRentalStart(),
                        item.getRentalEnd(),
                        item.getPricePerUnit() * 12 * item.getRentalPeriodYears() * item.getQuantity(),
                        item.getStatus()
                )).toList();

        return new PageImpl<>(itemResponses, pageable, rentalItemsPage.getTotalElements());
    }

    // 응답 DTO 변환
    @Transactional(readOnly = true)
    public RentalResponse convertToResponse(Rental rental) {
        List<RentalResponse.RentalItemResponse> itemResponses = rental.getItems().stream()
                .map(item -> new RentalResponse.RentalItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPricePerUnit(),
                        item.getRentalPeriodYears(),
                        item.getRentalStart(),
                        item.getRentalEnd(),
                        item.getPricePerUnit() * 12 * item.getRentalPeriodYears() * item.getQuantity(),
                        item.getStatus()
                )).toList();

        return new RentalResponse(
                rental.getId(),
                rental.getCreatedAt(),
                rental.getTotalPrice(),
                itemResponses
        );
    }

    // 상태별 총 아이템 수
    @Transactional(readOnly = true)
    public long countItemsByStatus(RentalStatus status) {
        return rentalItemRepository.countByStatus(status);
    }
}