package com.rental.test;

import com.rental.constant.Role;
import com.rental.inquiry.Inquiry;
import com.rental.inquiry.InquiryRepository;
import com.rental.inquiryComment.InquiryComment;
import com.rental.inquiryComment.InquiryCommentRepository;
import com.rental.member.Member;
import com.rental.member.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class InquiryCommentTest {

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private InquiryCommentRepository inquiryCommentRepository;

    @Autowired
    private MemberRepository memberRepository;

    private static final Random random = new Random();

    private static final String[] SAMPLE_COMMENTS = {
            "문의 주셔서 감사합니다. 확인 후 안내드립니다.",
            "말씀하신 내용은 정상적인 현상이며 안심하셔도 됩니다.",
            "불편을 겪게 해드려 죄송합니다. 해당 부분은 개선 중입니다.",
            "문의하신 사항은 담당 부서로 전달하였습니다.",
            "추가 문의사항이 있으시면 언제든지 연락주시기 바랍니다.",
            "문의 남겨주신 부분은 처리 완료되었습니다.",
            "도움이 되셨길 바랍니다. 감사합니다."
    };

    @Test
    void insertAdminComments() {

        // 관리자 계정 조회
        List<Member> admins = memberRepository.findByRole(Role.ADMIN);
        if (admins.isEmpty()) {
            System.out.println("❌ 관리자 계정이 없습니다. 테스트 종료.");
            return;
        }

        List<Inquiry> inquiries = inquiryRepository.findAll();
        int total = inquiries.size();

        int commented = 0;
        int skipped = 0;

        for (Inquiry inquiry : inquiries) {

            // 30일 이내 작성된 문의에는 댓글 X
            if (inquiry.getCreatedAt().isAfter(LocalDateTime.now().minusDays(30))) {
                skipped++;
                continue;
            }

            // 이미 댓글이 존재하면 스킵
            if (inquiry.getAdminComment() != null) {
                skipped++;
                continue;
            }

            InquiryComment comment = InquiryComment.builder()
                    .inquiry(inquiry)
                    .admin(admins.get(random.nextInt(admins.size())))
                    .comment(SAMPLE_COMMENTS[random.nextInt(SAMPLE_COMMENTS.length)])
                    .createdAt(LocalDateTime.now().minusDays(random.nextInt(20))) // 최근 20일 내 댓글 생성
                    .build();

            inquiryCommentRepository.save(comment);
            commented++;
        }

        System.out.println("========== 관리자 댓글 생성 완료 ==========");
        System.out.println("총 문의 수: " + total);
        System.out.println("생성된 관리자 댓글 수: " + commented);
        System.out.println("스킵된 문의 (최근글/이미 답변 있음): " + skipped);
    }
}
