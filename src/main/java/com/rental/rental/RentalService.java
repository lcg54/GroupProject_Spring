package com.rental.rental;

import com.rental.constant.RentalStatus;
import com.rental.review.RentalResponse;
import com.rental.member.Member;
import com.rental.product.Product;
import com.rental.member.MemberRepository;
import com.rental.product.ProductRepository;
import com.rental.util.PriceCalculator;
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
    private final PriceCalculator calculator;

    // 대여 생성 (createdAt 자동 설정)
    @Transactional
    public Rental createRental(RentalRequest request) {
        return createRentalWithDate(request, null);
    }

    // 대여 생성 (createdAt 수동 설정 - 테스트용)
    @Transactional
    public Rental createRentalWithDate(RentalRequest request, LocalDateTime createdAt) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Rental rental = new Rental();
        rental.setMember(member);

        // createdAt이 제공되면 사용, 아니면 현재 시간
        if (createdAt != null) {
            rental.setCreatedAt(createdAt);
        }

        rentalRepository.save(rental);

        int totalPrice = 0;
        List<RentalItem> items = new ArrayList<>();

        for (RentalRequest.RentalItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

            if (product.getAvailableStock() < itemReq.getQuantity()) {
                throw new IllegalArgumentException("상품 재고가 부족합니다: " + product.getName());
            }

            // 대여료 계산
            int monthlyPrice = calculator.calculateMonthlyPrice(product.getPrice(), itemReq.getPeriodYears());
            int itemTotal = calculator.calculateTotalPrice(monthlyPrice, itemReq.getPeriodYears(), itemReq.getQuantity());

            RentalItem item = new RentalItem();
            item.setRental(rental);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setMonthlyPrice(monthlyPrice);
            item.setRentalPeriodYears(itemReq.getPeriodYears());
            item.setRentalStart(itemReq.getRentalStart());
            item.setRentalEnd(itemReq.getRentalStart().plusYears(itemReq.getPeriodYears()));

            LocalDate today = LocalDate.now();
            int qty = itemReq.getQuantity();
            LocalDate startDate = item.getRentalStart();
            LocalDate sixYearsAgo = today.minusYears(6);
            // 테스트 데이터 주입용 분기. 주입 이후로는 else만 동작
            if (startDate.isBefore(sixYearsAgo)) { // 6년 이상 지난 주문은 반납 완료 처리
                item.setStatus(RentalStatus.RETURNED);
            } else if (startDate.isBefore(today)) { // 오늘 이전이면 대여중 처리
                item.setStatus(RentalStatus.RENTED);
                product.setRentedStock(product.getRentedStock() + qty);
            } else { // 오늘 이후면 예약중 처리
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
                        item.getProduct().getMainImage()
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
                        item.getMonthlyPrice(),
                        item.getRentalPeriodYears(),
                        item.getRentalStart(),
                        item.getRentalEnd(),
                        calculator.calculateTotalPrice(item.getMonthlyPrice(), item.getRentalPeriodYears(), item.getQuantity()),
                        item.getStatus(),
                        item.getProduct().getMainImage()
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

    // 예약 취소
    @Transactional
    public String cancelRentalItem(Long rentalItemId) {
        RentalItem item = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 대여 상품을 찾을 수 없습니다."));

        if (item.getStatus() != RentalStatus.RESERVED) {
            throw new IllegalStateException("예약 중인 상품만 취소할 수 있습니다.");
        }

        Product product = item.getProduct();
        int qty = item.getQuantity();

        // 예약 재고 감소 및 일반 재고 복구
        product.setReservedStock(Math.max(product.getReservedStock() - qty, 0));
        productRepository.save(product);

        item.setStatus(RentalStatus.CANCELED);
        rentalItemRepository.save(item);

        return "상품 예약이 취소되었습니다.";
    }

    // 반납 요청 (관리자 승인 대기)
    @Transactional
    public String requestReturn(Long rentalItemId) {
        RentalItem item = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 대여 상품을 찾을 수 없습니다."));

        if (item.getStatus() != RentalStatus.RENTED) {
            throw new IllegalStateException("대여 중인 상품만 반납을 요청할 수 있습니다.");
        }

        item.setStatus(RentalStatus.RETURN_REQUESTED);
        rentalItemRepository.save(item);

        return "반납 요청이 접수되었습니다.";
    }

    // 리뷰를 쓰지 않은 대여 내역 조회
    @Transactional(readOnly = true)
    public List<RentalResponse> getUnreviewedRentalsByMemberId(Long memberId) {
        List<Rental> rentals = rentalRepository.findRentalsByMemberId(memberId);

        List<RentalResponse> responses = rentals.stream()
                .map(rental -> {
                    List<RentalResponse.RentalItemResponse> unreviewedItems = rental.getItems().stream()
                            .filter(item -> item.getReview() == null)  // 리뷰 없는 항목만
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
                                    item.getProduct().getMainImage()
                            ))
                            .toList();

                    if (unreviewedItems.isEmpty()) return null; // 리뷰 없는 항목 없으면 제외

                    return new RentalResponse(
                            rental.getId(),
                            rental.getCreatedAt(),
                            rental.getTotalPrice(),
                            unreviewedItems
                    );
                })
                .filter(r -> r != null)
                .toList();

        return responses;
    }
}