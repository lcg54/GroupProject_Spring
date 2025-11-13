package com.rental.cart;

import com.rental.member.MemberRepository;
import com.rental.product.Product;
import com.rental.product.ProductRepository;
import com.rental.rental.*;
import com.rental.rental.find.FindRentalService;
import com.rental.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final RentalService rentalService;
    private final FindRentalService findRentalService;
    private final PriceCalculator priceCalculator;

    // 장바구니 추가
    @Transactional
    public Cart addToCart(CartRequest request) {
        Cart cart = cartRepository.findByMemberId(request.getMemberId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setMember(memberRepository.findById(request.getMemberId()).orElseThrow());
                    return cartRepository.save(newCart);
                });

        for (CartRequest.CartItemRequest itemReq : request.getItems()) {
            // 이미 장바구니에 동일 상품이 있으면 수량/기간 업데이트
            CartItem existingItem = cart.getItems().stream()
                    .filter(ci -> ci.getProduct().getId().equals(itemReq.getProductId()))
                    .findFirst()
                    .orElse(null);

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow();

            int estimatedPrice = priceCalculator.calculateMonthlyPrice(product.getPrice(), itemReq.getPeriodYears());

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + itemReq.getQuantity());
                existingItem.setPeriodYears(itemReq.getPeriodYears());
                existingItem.setRentalStart(itemReq.getRentalStart());
                existingItem.setEstimatedPrice(estimatedPrice);
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProduct(product);
                newItem.setQuantity(itemReq.getQuantity());
                newItem.setPeriodYears(itemReq.getPeriodYears());
                newItem.setRentalStart(itemReq.getRentalStart());
                newItem.setEstimatedPrice(estimatedPrice);
                cart.getItems().add(newItem);
            }
        }
        return cartRepository.save(cart);
    }

    // dto 변환
    @Transactional(readOnly = true)
    public CartResponse convertToResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream().map(item ->
                new CartResponse.CartItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getBrand().name(),
                        item.getQuantity(),
                        item.getPeriodYears(),
                        item.getRentalStart(),
                        item.getEstimatedPrice(),
                        item.getProduct().getMainImage()
                )
        ).toList();

        return new CartResponse(cart.getId(), cart.getCreatedAt(), itemResponses);
    }

    // 장바구니 조회
    @Transactional(readOnly = true)
    public Cart getCartByMember(Long memberId) {
        return cartRepository.findByMemberId(memberId)
                .orElseGet(() -> {
                    Cart emptyCart = new Cart();
                    emptyCart.setMember(memberRepository.findById(memberId).orElse(null));
                    emptyCart.setItems(List.of());
                    return emptyCart;
                });
    }

    // 장바구니 상품 제거
    @Transactional
    public void removeItem(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
    }

    // 장바구니에서 주문 생성
    @Transactional
    public RentalResponse createRentalFromCart(Long cartId, List<RentalRequest.RentalItemRequest> selectedItems) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니가 존재하지 않습니다."));

        // 선택된 상품 중 대여 시작일 체크
        boolean hasNullStartDate = selectedItems.stream()
                .anyMatch(item -> item.getRentalStart() == null);
        if (hasNullStartDate) {
            throw new IllegalArgumentException("대여 시작일이 지정되지 않은 상품이 있습니다. 주문 전 대여 시작일을 선택해주세요.");
        }

        // Rental 생성
        Rental rental = rentalService.createRental(
                new RentalRequest() {{
                    setMemberId(cart.getMember().getId());
                    setItems(selectedItems);
                }}
        );

        // 주문 생성 후 카트에서 해당 상품 제거
        cart.getItems().removeIf(ci -> selectedItems.stream()
                .anyMatch(si -> si.getProductId().equals(ci.getProduct().getId())));
        cartRepository.save(cart);

        // DTO 반환
        return findRentalService.convertToResponse(rental);
    }

    // 관리자용: 전체 회원 장바구니 요약 조회
    @Transactional(readOnly = true)
    public List<CartSummaryResponse> getCartSummary() {
        List<Cart> allCarts = cartRepository.findAll();

        // 모든 cart의 item들을 평탄화(flatten)
        List<CartItem> allItems = allCarts.stream()
                .flatMap(cart -> cart.getItems().stream())
                .toList();

        // 상품별로 그룹핑하면서 총 수량과 총 월 납부액을 계산
        Map<Product, CartSummaryAggregate> grouped = allItems.stream()
                .collect(Collectors.groupingBy(
                        CartItem::getProduct,
                        Collectors.reducing(
                                new CartSummaryAggregate(0, 0),
                                item -> new CartSummaryAggregate(
                                        item.getQuantity(),
                                        item.getEstimatedPrice() * item.getQuantity() // 월 납부액 합계
                                ),
                                (a, b) -> new CartSummaryAggregate(
                                        a.totalQuantity + b.totalQuantity,
                                        a.totalEstimatedPrice + b.totalEstimatedPrice
                                )
                        )
                ));

        // DTO 변환
        return grouped.entrySet().stream()
                .map(entry -> {
                    Product product = entry.getKey();
                    CartSummaryAggregate agg = entry.getValue();

                    // 상품별 월 납부액 평균 계산 (총액 ÷ 수량)
                    int averageMonthlyPrice = agg.totalQuantity > 0
                            ? agg.totalEstimatedPrice / agg.totalQuantity
                            : 0;

                    return new CartSummaryResponse(
                            product.getId(),
                            product.getName(),
                            product.getBrand().name(),
                            averageMonthlyPrice,
                            product.getMainImage(),
                            agg.totalQuantity
                    );
                })
                .toList();
    }

    // 내부 집계용 클래스
    private static class CartSummaryAggregate {
        int totalQuantity;
        int totalEstimatedPrice;

        CartSummaryAggregate(int totalQuantity, int totalEstimatedPrice) {
            this.totalQuantity = totalQuantity;
            this.totalEstimatedPrice = totalEstimatedPrice;
        }
    }

}