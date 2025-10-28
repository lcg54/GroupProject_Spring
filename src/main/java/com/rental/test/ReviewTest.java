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

        double reviewRate = 0.9;

        // 각 상품별 기존 리뷰 수 미리 조회
        Map<Long, Long> reviewCountMap = new HashMap<>();
        reviewRepository.findAll().forEach(r ->
                reviewCountMap.merge(r.getProduct().getId(), 1L, Long::sum)
        );

        for (RentalItem item : rentalItems) {
            if (random.nextDouble() > reviewRate) continue;
            if (item.getRental() == null || item.getRental().getMember() == null) continue;
            Member member = item.getRental().getMember();
            Product product = item.getProduct();
            if (member == null || product == null) continue;

            boolean alreadyPlanned = reviewList.stream()
                    .anyMatch(r -> r.getMember().equals(member) && r.getProduct().equals(product));
            if (alreadyPlanned) continue;

            // 상품별 기존 리뷰 수 확인
            long existingReviews = reviewCountMap.getOrDefault(product.getId(), 0L);

            // 리뷰 많은 상품일수록 평균이 높아지게 가중치 적용
            double baseRating = 1.0 + random.nextDouble() * 4.0; // 1.0 ~ 5.0
            double popularityBoost = Math.min(existingReviews / 20.0, 0.5); // 리뷰 20개당 +0.5까지 제한
            double adjustedRating = Math.min(5.0, baseRating + popularityBoost);
            adjustedRating = Math.round(adjustedRating * 2) / 2.0; // 0.5 단위 반올림

            String title = getRandomTitle();
            String content = getRandomContent();

            LocalDateTime baseDate;
            if (item.getRentalEnd() != null) {
                baseDate = item.getRentalEnd().atStartOfDay();
            } else if (item.getRental() != null && item.getRental().getCreatedAt() != null) {
                baseDate = item.getRental().getCreatedAt();
            } else {
                baseDate = now.minusDays(random.nextInt(60));
            }

            LocalDateTime randomDate = baseDate.plusDays(1 + random.nextInt(30));

            // 리뷰 작성일이 오늘보다 미래면 현재 시점 근처로 조정
            if (randomDate.isAfter(now)) {
                randomDate = now.minusDays(random.nextInt(3));
            }

            Review review = Review.builder()
                    .product(product)
                    .member(member)
                    .rating(adjustedRating)
                    .title(title)
                    .content(content)
                    .regDate(randomDate)
                    .build();

            reviewList.add(review);

            // 리뷰 생성 후 카운트 업데이트
            reviewCountMap.merge(product.getId(), 1L, Long::sum);
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