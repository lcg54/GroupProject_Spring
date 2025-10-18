package com.rental.service;

import com.rental.constant.RentalStatus;
import com.rental.entity.Member;
import com.rental.entity.Product;
import com.rental.entity.Rental;
import com.rental.entity.RentalItem;
import com.rental.repository.MemberRepository;
import com.rental.repository.ProductRepository;
import com.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RentalService {
    private final RentalRepository rentalRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Rental createRental(Long memberId, Long productId, int periodYears) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        // 가격 계산 로직 (임시로 짠거)
        int basePrice = product.getPrice();
        int monthlyPrice = (int) (basePrice / (periodYears * 10.0) - 1100);
        int totalPrice = monthlyPrice * 12 * periodYears;

        RentalItem item = new RentalItem();

        Rental rental = new Rental();
        rental.setMember(member);
        rental.setRentalPeriodYears(periodYears);
        rental.setMonthlyPrice(monthlyPrice);
        rental.setTotalPrice(totalPrice);
        rental.setRentalStart(LocalDate.now());
        rental.setRentalEnd(LocalDate.now().plusYears(periodYears));
        rental.setStatus(RentalStatus.RESERVED);

        item.setRental(rental);
        item.setProduct(product);
        item.setQuantity(1);
        item.setPricePerUnit(monthlyPrice);

        rental.getItems().add(item);
        rentalRepository.save(rental);

        return rental;
    }
}
