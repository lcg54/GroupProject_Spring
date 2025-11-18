package com.rental.test;

import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.review.Review;
import com.rental.review.ReviewRepository;
import com.rental.review.recommend.ReviewRecommend;
import com.rental.review.recommend.ReviewRecommendRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@SpringBootTest
public class ReviewRecommendTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReviewRecommendRepository reviewRecommendRepository;

    private static final Random random = new Random();

    @Test
    @Transactional
    @Rollback(false)
    void insertSampleReviewRecommends() {

        List<Review> reviews = reviewRepository.findAll();
        List<Member> members = memberRepository.findAll();

        if (reviews.isEmpty() || members.isEmpty()) {
            System.out.println("데이터 부족 → 추천 주입 불가");
            return;
        }

        List<ReviewRecommend> recommendList = new ArrayList<>();

        for (Review review : reviews) {

            int base = (int) Math.round(review.getRating());  // 평점 기반 (최대 5)
            int randomAdd = random.nextInt(4);               // + 0~3 랜덤
            int recommendCount = base + randomAdd;           // 총 추천 수

            Set<Long> selectedMembers = new HashSet<>();

            for (int i = 0; i < recommendCount; i++) {

                Member member = members.get(random.nextInt(members.size()));

                // 같은 회원이 같은 리뷰에 중복 추천하지 않도록 처리
                if (selectedMembers.contains(member.getId())) continue;
                selectedMembers.add(member.getId());

                ReviewRecommend rr = ReviewRecommend.builder()
                        .member(member)
                        .review(review)
                        .build();

                recommendList.add(rr);

                // ⬇️ 실제 서비스와 동일한 방식으로 추천 수 증가
                review.setRecommend(review.getRecommend() + 1);
            }

            // 리뷰 엔티티 저장
            reviewRepository.save(review);
        }

        if (!recommendList.isEmpty()) {
            reviewRecommendRepository.saveAll(recommendList);
        }

        System.out.println("✅ 리뷰 추천 " + recommendList.size() + "개가 생성되었습니다.");
    }
}