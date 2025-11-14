package com.rental.inquiryComment;

import com.rental.constant.Role;
import com.rental.inquiry.Inquiry;
import com.rental.inquiry.InquiryRepository;
import com.rental.member.Member;
import com.rental.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryCommentService {
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository commentRepository;
    private final MemberRepository memberRepository;

    // 관리자 답변 등록
    public InquiryCommentResponse createAdminComment(Long inquiryId, Long adminId, String commentText) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의글입니다."));
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new IllegalStateException("관리자만 답변을 작성할 수 있습니다.");
        }
        if (commentRepository.findByInquiryId(inquiryId) != null) {
            throw new IllegalStateException("이미 답변이 등록된 문의입니다.");
        }

        InquiryComment comment = InquiryComment.builder()
                .inquiry(inquiry)
                .admin(admin)
                .comment(commentText)
                .build();

        inquiry.setAdminComment(comment);
        InquiryComment saved = commentRepository.save(comment);

        return InquiryCommentResponse.builder()
                .admin(saved.getAdmin().getName())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // 관리자 답변 수정
    public void updateAdminComment(Long inquiryId, Long requesterId, String commentText) {
        InquiryComment comment = commentRepository.findByInquiryId(inquiryId);

        if (comment == null) {
            throw new IllegalArgumentException("답변이 존재하지 않습니다.");
        }

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!requester.getRole().equals(Role.ADMIN)) {
            throw new IllegalStateException("관리자만 답변을 수정할 수 있습니다.");
        }

        comment.setComment(commentText);
        commentRepository.save(comment);
    }

    // 관리자 답변 삭제
    public void deleteAdminComment(Long inquiryId, Long requesterId) {
        InquiryComment comment = commentRepository.findByInquiryId(inquiryId);

        if (comment == null) {
            throw new IllegalArgumentException("답변이 존재하지 않습니다.");
        }

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        if (!requester.getRole().equals(Role.ADMIN)) {
            throw new IllegalStateException("관리자만 답변을 삭제할 수 있습니다.");
        }

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의글입니다."));

        inquiry.setAdminComment(null);
        commentRepository.delete(comment);
    }
}