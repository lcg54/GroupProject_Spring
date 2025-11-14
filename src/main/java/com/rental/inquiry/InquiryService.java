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

    // 문의글 수정 (본인만 가능)
    public void updateInquiry(Long inquiryId, Long requesterId, String title, String content) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 본인 확인 (관리자는 수정 불가)
        if (!inquiry.getMember().getId().equals(requesterId)) {
            throw new IllegalStateException("본인의 문의글만 수정할 수 있습니다.");
        }

        if (requester.getRole().equals(Role.ADMIN)) {
            throw new IllegalStateException("관리자는 문의글을 수정할 수 없습니다.");
        }

        inquiry.setTitle(title);
        inquiry.setContent(content);
        inquiryRepository.save(inquiry);
    }

    // 문의글 삭제 (본인 또는 관리자)
    public void deleteInquiry(Long inquiryId, Long requesterId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 문의입니다."));

        Member requester = memberRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 본인 또는 관리자만 삭제 가능
        boolean isOwner = inquiry.getMember().getId().equals(requesterId);
        boolean isAdmin = requester.getRole().equals(Role.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new IllegalStateException("본인의 문의글이거나 관리자만 삭제할 수 있습니다.");
        }

        inquiryRepository.delete(inquiry);
    }

    // 관리자 전체 문의 조회
    public Page<InquiryResponse> getAllInquiries(Pageable pageable, Boolean answered) {
        Page<Inquiry> inquiries;

        if (answered == null) {
            // 전체 조회
            inquiries = inquiryRepository.findAll(pageable);
        } else if (answered) {
            // 답변 완료된 문의만
            inquiries = inquiryRepository.findByAdminCommentIsNotNull(pageable);
        } else {
            // 답변 대기 중인 문의만
            inquiries = inquiryRepository.findByAdminCommentIsNull(pageable);
        }

        return inquiries.map(this::convertToDto);
    }
}