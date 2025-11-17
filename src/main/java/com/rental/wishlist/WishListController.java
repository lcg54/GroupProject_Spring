package com.rental.wishlist;

import com.rental.member.MemberService;
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
    private final WishListService wishListService;
    private final MemberService memberService;

    // 현재 찜 상태 확인
    @GetMapping("/status")
    public Map<String, Object> status(
            @RequestParam Long memberId,
            @RequestParam Long productId
    ) {
        boolean wished = wishListService.isWished(memberId, productId);
        return Map.of("wished", wished);
    }

    // 찜 전환
    @PostMapping("/toggle")
    public Map<String, Object> toggle(@Valid @RequestBody WishListRequest req) {
        if(memberService.isAdmin(req.getMemberId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자는 찜할 수 없습니다.");
        }
        boolean wished = wishListService.toggle(req.getMemberId(), req.getProductId());
        return Map.of("wished", wished);
    }

    // 내 찜 목록
    @GetMapping("/my")
    public List<Long> my(@RequestParam Long memberId) {
        return wishListService.getMyProductIds(memberId);
    }

    // 선택 삭제 (여러 상품 삭제)
    @PostMapping("/delete-selected")
    public Map<String, Object> deleteSelected(@Valid @RequestBody DeleteSelectedRequest req) {
        if (memberService.isAdmin(req.getMemberId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자는 찜을 삭제할 수 없습니다.");
        }
        int deletedCount = wishListService.deleteSelected(req.getMemberId(), req.getProductIds());
        return Map.of("deletedCount", deletedCount);
    }
}