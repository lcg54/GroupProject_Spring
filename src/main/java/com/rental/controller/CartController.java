package com.rental.controller;

import com.rental.dto.CartRequest;
import com.rental.dto.CartResponse;
import com.rental.entity.Cart;
import com.rental.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(@RequestBody CartRequest request) {
        Cart cart = cartService.addToCart(request);
        return ResponseEntity.ok(cartService.convertToResponse(cart));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long memberId) {
        Cart cart = cartService.getCartByMember(memberId);
        return ResponseEntity.ok(cartService.convertToResponse(cart));
    }

    @DeleteMapping("/{cartId}/product/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long cartId, @PathVariable Long productId) {
        cartService.removeItem(cartId, productId);
        return ResponseEntity.noContent().build();
    }
}