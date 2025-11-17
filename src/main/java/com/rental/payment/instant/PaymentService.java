package com.rental.payment.instant;

import com.rental.constant.PaymentStatus;
import com.rental.constant.RentalStatus;
import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.payment.subscription.SubscriptionRepository;
import com.rental.product.Product;
import com.rental.product.ProductRepository;
import com.rental.rental.Rental;
import com.rental.rental.RentalItem;
import com.rental.rental.RentalItemRepository;
import com.rental.rental.RentalRepository;
import com.rental.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final RentalRepository rentalRepository;
    private final RentalItemRepository rentalItemRepository;
    private final PriceCalculator calculator;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${toss.secret-key}")
    private String secretKey;

    // 결제 준비 - 결제 요청 시 Toss에 전달할 orderId, 금액, 사용자명, 아이템 정보만 응답
    public PaymentReadyResponse preparePayment(PaymentReadyRequest request, String username) {
        Member member = memberRepository.findByUsername(username);
        if (member == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }

        // 실제 DB에 저장하지 않고, Toss 결제용 임시 주문 ID 생성
        String orderId = "order-" + System.currentTimeMillis();

        return new PaymentReadyResponse(
                orderId,
                request.getTotalAmount(),
                member.getName(),
                request.getItems()
        );
    }

    // 결제 승인 (결제 성공 후 Rental, RentalItem 생성)
    @Transactional
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        String url = "https://api.tosspayments.com/v1/payments/confirm";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(secretKey, "");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("paymentKey", request.getPaymentKey());
        body.put("orderId", request.getOrderId());
        body.put("amount", request.getAmount());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        // Rental 생성
        Member member = memberRepository.findByUsername(request.getUsername());
        if (member == null) throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");

        Rental rental = new Rental();
        rental.setMember(member);
        rental.setCreatedAt(LocalDateTime.now());
        rental.setTotalPrice(request.getAmount());
        rentalRepository.save(rental);

        // RentalItem 생성
        for (PaymentConfirmRequest.Item itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

            RentalItem item = new RentalItem();
            item.setRental(rental);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setMonthlyPrice(calculator.calculateMonthlyPrice(product.getPrice(), itemReq.getPeriodYears()));
            item.setRentalPeriodYears(itemReq.getPeriodYears());
            item.setPaymentStatus(PaymentStatus.PAID);
            item.setStatus(RentalStatus.RESERVED);

            rentalItemRepository.save(item);
        }

        return new PaymentConfirmResponse("success", response.getBody());
    }
}