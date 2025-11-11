package com.rental.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** DB 접근(조회/저장/삭제) */
public interface WishListRepository extends JpaRepository<WishList, Long> {
    List<WishList> findAllByMemberId(Long memberId);
    boolean existsByMemberIdAndProductId(Long memberId, Long productId);
    void deleteByMemberIdAndProductId(Long memberId, Long productId);
}
