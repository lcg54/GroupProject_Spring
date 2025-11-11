package com.rental.wishlist;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
public class WishListService {

    private final WishListRepository wishListRepository;

    public WishListService(WishListRepository wishListRepository) {
        this.wishListRepository = wishListRepository;
    }

    // 회원이 이 상품을 찜했는지
    @Transactional(readOnly = true)
    public boolean isWished(Long memberId, Long productId) {
        return wishListRepository.existsByMemberIdAndProductId(memberId, productId);
    }

    // 찜하기 <-> 해제하기
    @Transactional
    public boolean toggle(Long memberId, Long productId) {
        boolean exists = wishListRepository.existsByMemberIdAndProductId(memberId, productId);
        if (exists) {
            wishListRepository.deleteByMemberIdAndProductId(memberId, productId);
            return false;
        } else {
            wishListRepository.save(new WishList(memberId, productId));
            return true;
        }
    }

    // 회원이 찜한 상품 목록만 뽑아오기
    @Transactional(readOnly = true)
    public List<Long> getMyProductIds(Long memberId) {
        List<WishList> wishListForMember = wishListRepository.findAllByMemberId(memberId);
        List<Long> result = new ArrayList<>();
        for (WishList w : wishListForMember) {
            result.add(w.getProductId());
        }
        return  result;
    }

}
