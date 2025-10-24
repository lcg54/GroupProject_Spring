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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Rental createRental(RentalRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        Rental rental = new Rental();
        rental.setMember(member);
        rental.setStatus(RentalStatus.RESERVED);
        rentalRepository.save(rental);

        int totalPrice = 0;
        List<RentalItem> items = new ArrayList<>();

        for (RentalRequest.RentalItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

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
            items.add(item);
            totalPrice += itemTotal;
        }
        rental.setItems(items);
        rental.setTotalPrice(totalPrice);
        rentalItemRepository.saveAll(items);
        return rentalRepository.save(rental);
    }

    // 응답 DTO 변환
    @Transactional(readOnly = true)
    public RentalResponse convertToResponse(Rental rental) {
        List<RentalResponse.RentalItemResponse> itemResponses = rental.getItems().stream().map(item ->
                new RentalResponse.RentalItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPricePerUnit(),
                        item.getRentalPeriodYears(),
                        item.getRentalStart(),
                        item.getRentalEnd(),
                        item.getPricePerUnit() * 12 * item.getRentalPeriodYears() * item.getQuantity()
                )
        ).toList();

        return new RentalResponse(
                rental.getId(),
                rental.getStatus(),
                rental.getCreatedAt(),
                rental.getTotalPrice(),
                itemResponses
        );
    }
}