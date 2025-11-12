package com.rental.wishlist;

import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.product.Product;
import com.rental.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishListService {
    private final WishListRepository wishListRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    // 회원이 이 상품을 찜했는지
    @Transactional(readOnly = true)
    public boolean isWished(Long memberId, Long productId) {
        return wishListRepository.existsByMemberIdAndProductId(memberId, productId);
    }

    // 찜하기 <-> 해제하기
    @Transactional
    public boolean toggle(Long memberId, Long productId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        boolean exists = wishListRepository.existsByMemberIdAndProductId(memberId, productId);
        if (exists) {
            wishListRepository.deleteByMemberIdAndProductId(memberId, productId);
            return false;
        } else {
            wishListRepository.save(new WishList(member, product));
            return true;
        }
    }

    // 회원이 찜한 상품 목록만 뽑아오기
    @Transactional(readOnly = true)
    public List<Long> getMyProductIds(Long memberId) {
        List<WishList> wishListForMember = wishListRepository.findAllByMemberId(memberId);
        List<Long> result = new ArrayList<>();
        for (WishList w : wishListForMember) {
            result.add(w.getProduct().getId());
        }
        return result;
    }
}