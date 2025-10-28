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
import java.time.LocalDateTime;
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

        LocalDate today = LocalDate.now();
        LocalDate sixYearsAgo = today.minusYears(6);

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

            LocalDate startDate = item.getRentalStart();
            int qty = itemReq.getQuantity();

            // 테스트 데이터 주입용 대여상태 분기
            if (startDate.isBefore(sixYearsAgo)) { // 6년 이상 지난 주문 → 반납 완료 (샘플)
                item.setStatus(RentalStatus.RETURNED);
            } else if (startDate.isBefore(today)) { // 오늘 이전 → 대여중 (샘플)
                item.setStatus(RentalStatus.RENTED);
                product.setRentedStock(product.getRentedStock() + qty);
            } else { // 오늘 또는 이후 → 예약중 (실제 주문)
                item.setStatus(RentalStatus.RESERVED);
                product.setReservedStock(product.getReservedStock() + qty);
            }

            productRepository.save(product);

            items.add(item);
            totalPrice += itemTotal;
        }

        rental.setItems(items);
        rental.setTotalPrice(totalPrice);

        // 전체 아이템 중 가장 빠른 rentalStart를 기준으로 주문 생성일 결정
        LocalDate earliestStart = items.stream()
                .map(RentalItem::getRentalStart)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDateTime createdAt;

        // 테스트 데이터 주입용 주문생성일 분기
        if (earliestStart.isBefore(today)) { // 샘플 데이터
            int daysBefore = 1 + (int) (Math.random() * 7); // 1~7일 전
            createdAt = earliestStart.atStartOfDay().minusDays(daysBefore);
        } else { // 실제 주문
            createdAt = LocalDateTime.now();
        }
        rental.setCreatedAt(createdAt);

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
        Sort sort;
        switch (status) {
            case RESERVED, SHIPPING -> sort = Sort.by(Sort.Direction.ASC, "rentalStart"); // 예약일(미래) 가까운 순
            case RENTED, REPAIR, LATE -> sort = Sort.by(Sort.Direction.ASC, "rentalEnd"); // 종료일(미래) 가까운 순
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

    // 회원별 대여 내역 조회
    @Transactional(readOnly = true)
    public List<RentalResponse> getRentalsByMemberId(Long memberId) {
        // @Query 방식 사용 (N+1 문제 방지)
        List<Rental> rentals = rentalRepository.findRentalsByMemberId(memberId);

        // 또는 간단한 쿼리 메서드 사용
        // List<Rental> rentals = rentalRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        return rentals.stream()
                .map(this::convertToResponse)
                .toList();
    }

    // 특정 대여 상세 조회
    @Transactional(readOnly = true)
    public RentalResponse getRentalById(Long rentalId) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("대여 정보를 찾을 수 없습니다."));
        return convertToResponse(rental);
    }
}