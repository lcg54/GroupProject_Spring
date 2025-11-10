package com.rental.cart;

import com.rental.rental.RentalRequest;
import com.rental.rental.RentalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    // 장바구니에 상품 추가
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@RequestBody CartRequest request) {
        Cart cart = cartService.addToCart(request);
        return ResponseEntity.ok(cartService.convertToResponse(cart));
    }

    // 장바구니 조회
    @PostMapping("/get")
    public ResponseEntity<CartResponse> getCart(@RequestBody Map<String, Long> request) {
        Long memberId = request.get("memberId");
        Cart cart = cartService.getCartByMember(memberId);
        return ResponseEntity.ok(cartService.convertToResponse(cart));
    }

    // 장바구니에서 상품 제거
    @DeleteMapping("/{cartId}/product/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long cartId, @PathVariable Long productId) {
        cartService.removeItem(cartId, productId);
        return ResponseEntity.noContent().build();
    }

    // 장바구니의 상품 주문
    @PostMapping("/{cartId}/rental")
    public ResponseEntity<RentalResponse> createRentalFromCart(
            @PathVariable Long cartId,
            @RequestBody RentalRequest rentalRequest
    ) {
        RentalResponse response = cartService.createRentalFromCart(cartId, rentalRequest.getItems());
        return ResponseEntity.ok(response);
    }

    // 관리자 전용: 전체 장바구니 요약 조회
    @GetMapping("/admin/summary")
    public ResponseEntity<List<CartSummaryResponse>> getCartSummary() {
        List<CartSummaryResponse> summary = cartService.getCartSummary();
        return ResponseEntity.ok(summary);
    }
}