package com.rental.service;

import com.rental.dto.CartRequest;
import com.rental.dto.CartResponse;
import com.rental.dto.RentalRequest;
import com.rental.entity.*;
import com.rental.repository.CartRepository;
import com.rental.repository.MemberRepository;
import com.rental.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final RentalService rentalService;

    @Transactional
    public Cart addToCart(CartRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));

        // 기존 장바구니가 있으면 재사용, 없으면 생성
        Cart cart = cartRepository.findByMemberId(request.getMemberId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setMember(member);
                    return cartRepository.save(newCart);
                });

        List<CartItem> items = new ArrayList<>();
        for (CartRequest.CartItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

            // 예상 대여료 계산 (Rental과 동일한 로직 재사용)
            int monthlyPrice = (int) (product.getPrice() / (itemReq.getPeriodYears() * 8.0) - 5100);
            int estimatedPrice = monthlyPrice * 12 * itemReq.getPeriodYears() * itemReq.getQuantity();

            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setPeriodYears(itemReq.getPeriodYears());
            item.setRentalStart(itemReq.getRentalStart());
            item.setEstimatedPrice(estimatedPrice);
            items.add(item);
        }

        cart.getItems().clear();
        cart.getItems().addAll(items);
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse convertToResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream().map(item ->
                new CartResponse.CartItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getEstimatedPrice(),
                        item.getPeriodYears(),
                        item.getRentalStart(),
                        item.getEstimatedPrice()
                )
        ).toList();

        return new CartResponse(cart.getId(), cart.getCreatedAt(), itemResponses);
    }

    @Transactional(readOnly = true)
    public Cart getCartByMember(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));
    }

    @Transactional
    public void removeItem(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
    }

    @Transactional
    public Rental createRentalFromCart(Long memberId) {
        Cart cart = getCartByMember(memberId); // Cart 조회

        // Cart → RentalRequest 변환
        RentalRequest rentalRequest = new RentalRequest();
        rentalRequest.setMemberId(memberId);

        List<RentalRequest.RentalItemRequest> items = cart.getItems().stream().map(item -> {
            RentalRequest.RentalItemRequest rItem = new RentalRequest.RentalItemRequest();
            rItem.setProductId(item.getProduct().getId());
            rItem.setQuantity(item.getQuantity());
            rItem.setPeriodYears(item.getPeriodYears());
            rItem.setRentalStart(item.getRentalStart());
            return rItem;
        }).toList();

        rentalRequest.setItems(items);

        // RentalService 호출
        Rental rental = rentalService.createRental(rentalRequest);

        // 주문 생성 후 카트 비우기
        cart.getItems().clear();
        cartRepository.save(cart);

        return rental;
    }
}