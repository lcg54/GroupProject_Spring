package com.rental.payment.fake;

import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.payment.billingKey.BillingKey;
import com.rental.payment.billingKey.BillingKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fake/payments")
@RequiredArgsConstructor
public class FakeCardController {
    private final BillingKeyRepository billingKeyRepository;
    private final MemberRepository memberRepository;
    private final CardRepository cardRepository;

    // 카드 등록 + BillingKey 발급
    @Transactional
    @PostMapping("/billing")
    public ResponseEntity<String> regBillingKey(@RequestBody FakeBillingRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));

        // 빌링키 없으면 주입
        if (billingKeyRepository.findByMemberId(member.getId()).isEmpty()){
            String billingKey = "TEST-BILLING-" + UUID.randomUUID();

            BillingKey bk = BillingKey.builder()
                    .member(member)
                    .billingKey(billingKey)
                    .customerKey(request.getCustomerKey())
                    .build();
            billingKeyRepository.save(bk);
        }

        Card card = Card.builder()
                .member(member)
                .cardNum(request.getCardNum())
                .build();
        cardRepository.save(card);

        return ResponseEntity.ok("카드가 등록되었습니다.");
    }

    // 카드 목록 조회
    @GetMapping("/cards/{memberId}")
    public ResponseEntity<?> getCards(@PathVariable Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));

        List<Card> cards = cardRepository.findAllByMemberId(memberId);

        return ResponseEntity.ok(cards);
    }

    // 카드 삭제
    @DeleteMapping("/card/{cardId}/{memberId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId, @PathVariable Long memberId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("카드를 찾을 수 없습니다"));

        cardRepository.delete(card);

        return ResponseEntity.noContent().build();
    }
}
