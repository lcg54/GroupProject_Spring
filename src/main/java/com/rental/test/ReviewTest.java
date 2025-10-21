package com.rental.test;

import com.rental.entity.Member;
import com.rental.entity.Product;
import com.rental.entity.Review;
import com.rental.repository.MemberRepository;
import com.rental.repository.ProductRepository;
import com.rental.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@SpringBootTest
public class ReviewTest {
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    private static final Random random = new Random();

    @Test
    void insertSampleReviews() {
        List<Product> products = productRepository.findAll();
        List<Member> members = memberRepository.findAll();

        if (products.isEmpty() || members.isEmpty()) {
            System.out.println("❌ 상품 또는 회원 데이터가 없습니다. 리뷰 생성을 건너뜁니다.");
            return;
        }

        long existing = reviewRepository.count();
        if (existing > 0) {
            System.out.println("이미 리뷰가 존재하므로 샘플 추가를 생략합니다. (현재 " + existing + "개)");
            return;
        }

        List<Review> reviewList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Product p : products) {
            int reviewCount = 10 + random.nextInt(191); // 10~200개
            double baseRating = 2.5 + 1.5 * (reviewCount / 200.0); // 리뷰 많으면 4점 근처, 적으면 2.5점 근처
            for (int i = 0; i < reviewCount; i++) {
                Member randomMember = members.get(random.nextInt(members.size()));

                // 각 리뷰별 랜덤 변동
                double rating = baseRating + (random.nextInt(5) - 2) * 0.5;
                // -1 ~ +1 범위, 0.5 단위
                rating = Math.max(1.0, Math.min(5.0, rating)); // 1~5 제한

                String title = getRandomTitle();
                String content = getRandomContent();

                // 작성일: 1년 전 ~ 오늘
                long minDay = now.minusYears(1).atZone(ZoneId.systemDefault()).toEpochSecond();
                long maxDay = now.atZone(ZoneId.systemDefault()).toEpochSecond();
                long randomEpoch = minDay + (long) (random.nextDouble() * (maxDay - minDay));
                LocalDateTime randomDate = LocalDateTime.ofEpochSecond(randomEpoch, 0, ZoneId.systemDefault().getRules().getOffset(now));

                Review review = Review.builder()
                        .product(p)
                        .member(randomMember)
                        .rating(rating)
                        .title(title)
                        .content(content)
                        .regDate(randomDate)
                        .build();

                reviewList.add(review);
            }
        }

        reviewRepository.saveAll(reviewList);
        System.out.println("✅ " + reviewList.size() + "개의 샘플 리뷰가 성공적으로 추가되었습니다.");
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