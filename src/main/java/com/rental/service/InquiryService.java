package com.rental.service;

import com.rental.constant.InquiryType;
import com.rental.entity.*;
import com.rental.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    private final InquiryCommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final RentalItemRepository rentalItemRepository;

    // 문의글 등록 (상품을 대여한 사용자만 가능, 대여당 1회만 가능)
    public Inquiry createInquiry(Long memberId, Long rentalItemId, String title, String content, InquiryType type) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        RentalItem rentalItem = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 대여 항목입니다."));

        // 본인이 대여한 상품인지 확인
        if (!rentalItem.getRental().getMember().getId().equals(memberId)) {
            throw new IllegalStateException("본인이 대여한 상품에 대해서만 문의를 작성할 수 있습니다.");
        }

        // 해당 rentalItem에 이미 문의가 존재하는지 확인
        boolean alreadyExists = inquiryRepository.existsByMemberIdAndRentalItemId(memberId, rentalItemId);
        if (alreadyExists) {
            throw new IllegalStateException("이미 이 대여 상품에 대해 문의를 작성했습니다.");
        }

        // 문의 생성
        Inquiry inquiry = Inquiry.builder()
                .member(member)
                .product(rentalItem.getProduct())
                .title(title)
                .content(content)
                .type(type)
                .build();

        inquiryRepository.save(inquiry);
        return inquiry;
    }

    // 상품별 문의글 조회
    @Transactional(readOnly = true)
    public List<Inquiry> getInquiriesByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        return inquiryRepository.findByProduct(product);
    }

    // 관리자 답변 등록
    public InquiryComment createAdminComment(Long adminId, Long inquiryId, String commentText) {
        Member admin = memberRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));
        if (!admin.getRole().name().equals("ADMIN")) {
            throw new IllegalStateException("관리자만 답변을 작성할 수 있습니다.");
        }

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의글을 찾을 수 없습니다."));

        if (commentRepository.findByInquiryId(inquiryId) != null) {
            throw new IllegalStateException("이미 답변이 등록된 문의입니다.");
        }

        InquiryComment comment = InquiryComment.builder()
                .inquiry(inquiry)
                .admin(admin)
                .comment(commentText)
                .build();

        return commentRepository.save(comment);
    }
}