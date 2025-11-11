package com.rental.inquiry;

import com.rental.constant.InquiryType;
import com.rental.constant.Role;
import com.rental.inquiryComment.InquiryComment;
import com.rental.inquiryComment.InquiryCommentResponse;
import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.product.Product;
import com.rental.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    String secretMessage = "비공개 처리된 게시물입니다.";

    // 상품별 문의글 조회
    public Page<InquiryResponse> getInquiriesByProduct(Long productId, Long memberId, Pageable pageable) {
        Page<Inquiry> inquiries = inquiryRepository.findByProductId(productId, pageable);

        // 비로그인 조회
        if (memberId == null) {
            return inquiries.map(inquiry -> {
                InquiryResponse response = convertToDto(inquiry);
                if (inquiry.isSecret()) {
                    response.setTitle(secretMessage);
                    response.setContent(secretMessage);
                    response.setAdminComment(maskAdminComment(inquiry.getAdminComment(), true));
                }
                return response;
            });
        }

        // 로그인 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return inquiries.map(inquiry -> {
            InquiryResponse response = convertToDto(inquiry);

            boolean hide = inquiry.isSecret()
                    && !inquiry.getMember().getId().equals(memberId)
                    && !member.getRole().equals(Role.ADMIN);

            if (hide) {
                response.setTitle(secretMessage);
                response.setContent(secretMessage);
                response.setSecret(true);
                response.setAdminComment(maskAdminComment(inquiry.getAdminComment(), true));
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
                .productId(inquiry.getProduct().getId())
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

    // 비공개 글일 경우 답변글도 비공개 처리
    private InquiryCommentResponse maskAdminComment(InquiryComment comment, boolean hide) {
        if (comment == null) return null;
        if (hide) {
            return InquiryCommentResponse.builder()
                    .admin(comment.getAdmin().getName())
                    .comment(secretMessage)
                    .createdAt(comment.getCreatedAt())
                    .build();
        } else {
            return InquiryCommentResponse.builder()
                    .admin(comment.getAdmin().getName())
                    .comment(comment.getComment())
                    .createdAt(comment.getCreatedAt())
                    .build();
        }
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

    // 회원별 문의글 조회
    public Page<InquiryResponse> getInquiriesByMemberId(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inquiry> inquiryPage = inquiryRepository.findByMemberId(memberId, pageable);

        return inquiryPage.map(this::convertToDto);
    }

}