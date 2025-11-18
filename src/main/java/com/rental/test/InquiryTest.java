package com.rental.test;

import com.rental.constant.InquiryType;
import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.product.Product;
import com.rental.product.ProductRepository;
import com.rental.inquiry.Inquiry;
import com.rental.inquiry.InquiryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class InquiryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    private static final Random random = new Random();

    private static final String[] SAMPLE_TITLES = {
            "제품 사용 관련 문의드립니다.",
            "배송 일정 문의",
            "구매 전 궁금한 점 있어요",
            "사이즈가 맞는지 질문드립니다.",
            "추가 구성품 문의",
            "고장 가능성 관련 질문",
            "재고 관련 문의",
            "반품 절차 문의",
            "장기 사용 시 문제 여부가 궁금합니다.",
            "기타 문의드립니다."
    };

    private static final String[] SAMPLE_CONTENTS = {
            "해당 상품을 오랫동안 사용해도 문제가 없을까요?",
            "배송이 얼마나 걸리는지 궁금합니다.",
            "사용 시 유의해야 할 점이 있을까요?",
            "구성품이 정확히 어떤 것들이 포함되나요?",
            "여러 개 구매하면 할인이 되나요?",
            "설치가 어려울 것 같아 문의드립니다.",
            "제품의 내구성이 어느 정도인지 궁금해요.",
            "환불 정책이 어떻게 되나요?",
            "추가 옵션이 있으면 안내 부탁드립니다.",
            "상세한 스펙을 알고 싶습니다."
    };

    @Test
    void insertSampleInquiries() {

        List<Member> members = memberRepository.findAll();
        List<Product> products = productRepository.findAll();

        if (members.isEmpty() || products.isEmpty()) {
            System.out.println("❌ 회원 또는 상품 데이터가 존재하지 않아 테스트를 중단합니다.");
            return;
        }

        int totalCreated = 0;

        for (Product product : products) {

            // 문의 개수: 상품당 5~10개
            int inquiryCount = 5 + random.nextInt(6);

            for (int i = 0; i < inquiryCount; i++) {

                Member randomMember = members.get(random.nextInt(members.size()));
                InquiryType randomType =
                        InquiryType.values()[random.nextInt(InquiryType.values().length)];

                Inquiry inquiry = Inquiry.builder()
                        .member(randomMember)
                        .product(product)
                        .title(SAMPLE_TITLES[random.nextInt(SAMPLE_TITLES.length)])
                        .content(SAMPLE_CONTENTS[random.nextInt(SAMPLE_CONTENTS.length)])
                        .type(randomType)
                        .isSecret(true) // 요청한 대로 모두 비밀글
                        .createdAt(LocalDateTime.now().minusDays(random.nextInt(120)))
                        .build();

                inquiryRepository.save(inquiry);
                totalCreated++;
            }
        }

        System.out.println("========== Inquiry 더미데이터 생성 완료 ==========");
        System.out.println("총 생성된 문의 수: " + totalCreated);
    }
}
