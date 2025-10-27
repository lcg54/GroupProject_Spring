package com.rental.test;

import com.rental.entity.Member;
import com.rental.entity.Product;
import com.rental.entity.RentalItem;
import com.rental.entity.Review;
import com.rental.repository.MemberRepository;
import com.rental.repository.RentalItemRepository;
import com.rental.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
public class ReviewTest {
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RentalItemRepository rentalItemRepository;

    @Autowired
    private MemberRepository memberRepository;

    private static final Random random = new Random();

    @Test
    @Transactional
    @Rollback(false)
    void insertSampleReviewsBasedOnActualRentals() {
        long existing = reviewRepository.count();
        if (existing > 0) {
            System.out.println("이미 리뷰가 존재하므로 샘플 추가를 생략합니다. (현재 " + existing + "개)");
            return;
        }

        List<RentalItem> rentalItems = rentalItemRepository.findAll();
        if (rentalItems.isEmpty()) {
            System.out.println("❌ 대여 아이템 데이터가 없습니다. 리뷰 생성을 건너뜁니다.");
            return;
        }

        List<Review> reviewList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 각 렌탈 아이템에 대해 리뷰 생성 확률 (90%)
        double reviewRate = 0.9;

        for (RentalItem item : rentalItems) {
            // 확률적으로 일부 아이템만 리뷰 작성
            if (random.nextDouble() > reviewRate) continue;

            // 작성 가능한 회원(주문자)과 상품 얻기
            if (item.getRental() == null || item.getRental().getMember() == null) continue;
            Member member = item.getRental().getMember();
            Product product = item.getProduct();
            if (member == null || product == null) continue;

            // 같은 실행 내 중복 생성 방지 (한 회원-상품 조합 당 한 건만)
            boolean alreadyPlanned = reviewList.stream()
                    .anyMatch(r -> r.getMember().equals(member) && r.getProduct().equals(product));
            if (alreadyPlanned) continue;

            // 평점 생성
            double rating = 1.0 + random.nextDouble() * 4.0; // 1.0 ~ 5.0
            rating = Math.round(rating * 2) / 2.0; // 0.5 단위 반올림

            String title = getRandomTitle();
            String content = getRandomContent();

            // 리뷰 작성일: rentalItem.rentalEnd 기준으로 1~30일 후 (rentalEnd 없으면 rental.createdAt 후)
            LocalDateTime baseDate;
            if (item.getRentalEnd() != null) {
                baseDate = item.getRentalEnd().atStartOfDay();
            } else if (item.getRental() != null && item.getRental().getCreatedAt() != null) {
                baseDate = item.getRental().getCreatedAt();
            } else {
                baseDate = now.minusDays(random.nextInt(60)); // 안전장치: 최근 시점
            }
            LocalDateTime randomDate = baseDate.plusDays(1 + random.nextInt(30));

            Review review = Review.builder()
                    .product(product)
                    .member(member)
                    .rating(rating)
                    .title(title)
                    .content(content)
                    .regDate(randomDate)
                    .build();

            reviewList.add(review);
        }

        if (!reviewList.isEmpty()) {
            reviewRepository.saveAll(reviewList);
        }
        System.out.println("✅ " + reviewList.size() + "개의 실제 주문 기반 리뷰가 성공적으로 추가되었습니다.");
    }

    private String getRandomTitle() {
        String[] titles = {
                "만족합니다!", "가격대비 최고네요", "조금 아쉬워요", "배송도 빠르고 좋아요", "재구매 의사 있습니다",
                "디자인이 예쁩니다", "성능이 생각보다 괜찮아요", "조용해서 좋아요", "기능이 다양해요", "추천합니다"
        };
        return titles[random.nextInt(titles.length)];
    }

    private String getRandomContent() {
        String[] contents = {
                "사용해보니 정말 만족스럽습니다.",
                "생각보다 튼튼하고 조용합니다.",
                "배송도 빠르고 포장 상태도 좋았어요.",
                "가성비가 아주 좋네요.",
                "디자인이 깔끔해서 인테리어에도 잘 어울립니다.",
                "조금 더 저렴했으면 좋겠어요.",
                "처음엔 고민했는데 사길 잘한 것 같아요.",
                "기능이 다양해서 편리합니다.",
                "설치 기사님도 친절했어요.",
                "추천받아서 샀는데 후회 없습니다."
        };
        return contents[random.nextInt(contents.length)];
    }
}