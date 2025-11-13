package com.rental.rental;

import com.rental.constant.PaymentStatus;
import com.rental.constant.RentalStatus;
import com.rental.member.Member;
import com.rental.payment.subscription.PaymentRecordRepository;
import com.rental.payment.subscription.SubscriptionRepository;
import com.rental.payment.subscription.SubscriptionService;
import com.rental.product.Product;
import com.rental.member.MemberRepository;
import com.rental.product.ProductRepository;
import com.rental.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RentalService {
    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final SubscriptionService subscriptionService;
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
                item.setPaymentStatus(PaymentStatus.END);
            } else if (startDate.isBefore(today)) { // 오늘 이전이면 대여중 처리
                item.setStatus(RentalStatus.RENTED);
                item.setPaymentStatus(PaymentStatus.PAID);
                product.setRentedStock(product.getRentedStock() + qty);
            } else { // 오늘 이후면 예약중 처리
                item.setStatus(RentalStatus.RESERVED);
                item.setPaymentStatus(PaymentStatus.UNPAID);
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

    // 예약 취소
    @Transactional
    public String cancelRentalItem(Long rentalItemId) {
        RentalItem item = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 대여 상품을 찾을 수 없습니다."));

        if (item.getStatus() != RentalStatus.RESERVED) {
            throw new IllegalStateException("예약 중인 상품만 취소할 수 있습니다.");
        }

        // 구독이 있는지 확인
        subscriptionRepository.findByRentalItemId(rentalItemId).ifPresent(sub -> {
            // 이미 첫 결제가 되었는지 판단: paymentRecord에서 성공 기록이 있으면 환불
            boolean hadSuccess = paymentRecordRepository.findAll().stream()
                    .anyMatch(r -> Objects.equals(r.getSubscriptionId(), sub.getId()) && r.isSuccess());

            if (hadSuccess) {
                subscriptionService.cancelSubscription(sub.getId()); // 자동 환불 및 구독 취소
            } else {
                subscriptionService.cancelSubscription(sub.getId()); // 단순 취소
            }
        });

        // 예약 재고 감소 및 일반 재고 복구
        Product product = item.getProduct();
        int qty = item.getQuantity();
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

    // 반납 요청 취소
    @Transactional
    public String cancelReturnRequest(Long rentalItemId) {
        RentalItem item = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 대여 상품을 찾을 수 없습니다."));

        if (item.getStatus() != RentalStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("반납 요청 상태인 상품만 취소할 수 있습니다.");
        }

        item.setStatus(RentalStatus.RENTED); // 다시 대여 중으로 복귀
        rentalItemRepository.save(item);

        return "반납 요청이 취소되었습니다.";
    }
}