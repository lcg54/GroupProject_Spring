package com.rental.payment.billingKey;

import com.rental.member.Member;
import com.rental.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class BillingKeyController {

    private final BillingKeyService billingKeyService;
    private final BillingKeyRepository billingKeyRepository;
    private final MemberRepository memberRepository;

    // (1) BillingKey 발급 요청
    @PostMapping("/billing")
    public ResponseEntity<String> issueBillingKey(@RequestBody BillingRequest request) {
        String billingKey = billingKeyService.requestBillingKey(request);
        return ResponseEntity.ok(billingKey);
    }

    // (2) BillingKey 저장
    @PostMapping("/billing-key")
    public ResponseEntity<?> saveBillingKey(@RequestBody Map<String, String> payload) {
        String billingKey = payload.get("billingKey");
        String customerKey = payload.get("customerKey");
        Long memberId = Long.valueOf(payload.get("memberId"));

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));

        BillingKey entity = BillingKey.builder()
                .member(member)
                .billingKey(billingKey)
                .customerKey(customerKey)
                .build();

        billingKeyRepository.save(entity);
        return ResponseEntity.ok("Billing key가 성공적으로 저장되었습니다.");
    }

    // 등록된 카드 조회
    @GetMapping("/cards/{customerKey}")
    public ResponseEntity<?> getCards(@PathVariable String customerKey) {
        List<Map<String, Object>> cards = billingKeyService.getCardsByCustomerKey(customerKey);
        return ResponseEntity.ok(Map.of("data", cards));
    }
}
