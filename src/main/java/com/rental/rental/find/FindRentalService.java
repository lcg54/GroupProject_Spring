package com.rental.rental.find;

import com.rental.rental.Rental;
import com.rental.rental.RentalRepository;
import com.rental.rental.RentalResponse;
import com.rental.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindRentalService {
    private final RentalRepository rentalRepository;
    private final PriceCalculator calculator;

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
                        item.getPaymentStatus(),
                        item.getProduct().getMainImage()
                )).toList();

        return new RentalResponse(
                rental.getId(),
                rental.getCreatedAt(),
                rental.getTotalPrice(),
                itemResponses
        );
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
                                    item.getPaymentStatus(),
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
