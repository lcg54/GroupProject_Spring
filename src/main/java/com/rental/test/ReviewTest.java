package com.rental.test;

import com.rental.entity.*;
import com.rental.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    @Autowired
    private ProductRepository productRepository;

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

        double reviewRate = 0.8; // 리뷰 작성 확률 80%
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

            // 이미 같은 회원이 같은 상품에 대해 리뷰 예정이라면 skip
            boolean alreadyPlanned = reviewList.stream()
                    .anyMatch(r -> r.getMember().equals(member) && r.getProduct().equals(product));
            if (alreadyPlanned) continue;

            long existingReviews = reviewCountMap.getOrDefault(product.getId(), 0L);

            // 평점 가중치 계산
            double baseRating = 1.0 + random.nextDouble() * 4.0;
            double popularityBoost = Math.min(existingReviews / 20.0, 0.5);
            double adjustedRating = Math.min(5.0, baseRating + popularityBoost);
            adjustedRating = Math.round(adjustedRating);

            String title = getRandomTitle();
            String content = getRandomContent();

            // 리뷰 날짜: 주문일 이후, 오늘 이전
            LocalDateTime orderDate = (item.getRental() != null && item.getRental().getCreatedAt() != null)
                    ? item.getRental().getCreatedAt()
                    : now.minusDays(random.nextInt(90));
            LocalDate orderLocalDate = orderDate.toLocalDate();
            LocalDate today = LocalDate.now();
            long daysBetween = Math.max(1, today.toEpochDay() - orderLocalDate.toEpochDay());
            LocalDateTime reviewDate = orderDate.plusDays(random.nextInt((int) daysBetween + 1));

            // 이미지 첨부 (임시로 카테고리 이미지 넣어둠)
            int imageCount = 1 + random.nextInt(3); // 1~3장
            List<ReviewImage> images = new ArrayList<>();
            for (int i = 0; i < imageCount; i++) {
                String fileName = "category_" + product.getCategory().name() + ".png";
                ReviewImage image = ReviewImage.builder()
                        .fileName(fileName)
                        .seq(i)
                        .build();
                images.add(image);
            }

            Review review = Review.builder()
                    .product(product)
                    .member(member)
                    .rentalItem(item)
                    .rating(adjustedRating)
                    .title(title)
                    .content(content)
                    .regDate(reviewDate)
                    .images(images)
                    .build();

            // 리뷰와 이미지 연결
            for (ReviewImage img : images) {
                img.setReview(review);
            }

            reviewList.add(review);
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