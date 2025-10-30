package com.rental.service;

import com.rental.constant.InquiryType;
import com.rental.constant.Role;
import com.rental.dto.InquiryCommentResponse;
import com.rental.dto.InquiryResponse;
import com.rental.entity.*;
import com.rental.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    // 상품별 문의글 조회
    public Page<InquiryResponse> getInquiriesByProduct(Long productId, Long memberId, Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findByProductId(productId, pageable);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return inquiries.map(inquiry -> {
            InquiryResponse response = convertToDto(inquiry);

            // 비공개 글이면 작성자와 관리자만 보이게
            if (inquiry.isSecret()
                    && !inquiry.getMember().getId().equals(memberId)
                    && !member.getRole().equals(Role.ADMIN))
            {
                response.setTitle("비공개 문의글입니다.");
                response.setContent("비공개 문의글입니다.");
                response.setSecret(true);
            }

            return response;
        });
    }

    // DTO 변환
    private InquiryResponse convertToDto(Inquiry inquiry) {
        InquiryComment comment = inquiry.getAdminComment();
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .memberId(inquiry.getMember().getId())
                .member(inquiry.getMember().getName())
                .type(inquiry.getType())
                .createdAt(inquiry.getCreatedAt())
                .isSecret(inquiry.isSecret())
                .adminComment(comment != null ? InquiryCommentResponse.builder()
                        .admin(comment.getAdmin().getName())
                        .comment(comment.getComment())
                        .createdAt(comment.getCreatedAt())
                        .build() : null)
                .build();
    }

    // 문의글 생성
    public Inquiry createInquiry(Long memberId, Long productId, String title, String content, InquiryType type, Boolean isSecret) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다."));

        Inquiry inquiry = Inquiry.builder()
                .member(member)
                .product(product)
                .title(title)
                .content(content)
                .type(type)
                .isSecret(isSecret)
                .build();

        return inquiryRepository.save(inquiry);
    }

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
}