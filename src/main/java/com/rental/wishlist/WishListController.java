package com.rental.wishlist;

import com.rental.member.MemberService;
import com.rental.product.ProductService;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishListController {

    private final WishListService wishlistservice;
    private final MemberService memberservice;
    private final ProductService productservice;

    // 현재 찜 상태 확인
    @GetMapping("/status")
    public Map<String, Object> status(
            @RequestParam Long memberId,
            @RequestParam Long productId
    ) {
        boolean wished = wishlistservice.isWished(memberId, productId);
        return Map.of("wished", wished);
    }

    // 찜 전환
    @PostMapping("/toggle")
    public Map<String, Object> toggle(@Valid @RequestBody WishListRequest req) {
        if(memberservice.isAdmin(req.getMemberId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자는 찜할 수 없습니다.");
        }
        boolean wished = wishlistservice.toggle(req.getMemberId(), req.getProductId());
        return Map.of("wished", wished);
    }

    @GetMapping("/my")
    public List<Long> my(@RequestParam Long memberId) {
        return wishlistservice.getMyProductIds(memberId);
    }
}