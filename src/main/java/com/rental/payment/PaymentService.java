package com.rental.payment;

import com.rental.constant.RentalStatus;
import com.rental.member.Member;
import com.rental.product.Product;
import com.rental.rental.Rental;
import com.rental.rental.RentalItem;
import com.rental.member.MemberRepository;
import com.rental.product.ProductRepository;
import com.rental.rental.RentalItemRepository;
import com.rental.rental.RentalRepository;
import com.rental.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Value("${toss.secret-key}")
    private String secretKey;

    // 1. 결제 준비 (주문 생성)
    @Transactional
    public PaymentReadyResponse preparePayment(PaymentReadyRequest request, String username) {
        Member member = memberRepository.findByUsername(username);
        if (member == null) {
            throw new IllegalArgumentException("회원 정보를 찾을 수 없습니다.");
        }

        // Rental 생성
        Rental rental = new Rental();
        rental.setMember(member);
        rental.setCreatedAt(LocalDateTime.now());
        rental.setTotalPrice(request.getTotalAmount());
        rentalRepository.save(rental);

        // RentalItem 생성
        for (PaymentReadyRequest.Item itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

            RentalItem item = new RentalItem();
            item.setRental(rental);
            item.setProduct(product);
            item.setQuantity(itemReq.getQuantity());
            item.setPricePerUnit(calculator.calculateMonthlyPrice(product.getPrice(), itemReq.getPeriodYears()));
            item.setRentalPeriodYears(itemReq.getPeriodYears());
            item.setStatus(RentalStatus.READY);
            rentalItemRepository.save(item);
        }

        return new PaymentReadyResponse("order-" + rental.getId(), rental.getTotalPrice(), member.getName());
    }

    // 2. 결제 승인
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

        // 승인 성공 시 DB 상태 업데이트
        Long rentalId = Long.parseLong(request.getOrderId().replace("order-", ""));
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new RuntimeException("주문 정보를 찾을 수 없습니다."));
        rental.setTotalPrice(request.getAmount());
        rental.getItems().forEach(i -> i.setStatus(RentalStatus.PAID));
        rentalRepository.save(rental);

        return new PaymentConfirmResponse("success", response.getBody());
    }
}
