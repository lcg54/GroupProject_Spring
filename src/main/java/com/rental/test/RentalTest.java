package com.rental.test;

import com.rental.constant.PaymentStatus;
import com.rental.member.Member;
import com.rental.member.MemberRepository;
import com.rental.payment.subscription.Subscription;
import com.rental.payment.subscription.SubscriptionRepository;
import com.rental.constant.SubscriptionStatus;
import com.rental.product.Product;
import com.rental.product.ProductRepository;
import com.rental.rental.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@SpringBootTest
public class RentalTest {

    @Autowired
    private RentalService rentalService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private RentalRepository rentalRepository;
    @Autowired
    private RentalItemRepository rentalItemRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private static final Random random = new Random();

    @Test
    void insertSampleRentalsWithSubscriptions() {
        List<Member> members = memberRepository.findAllByIdBetween(2L, 301L); // 일반회원 전체
        List<Product> products = productRepository.findAll();

        LocalDate today = LocalDate.now();
        int successCount = 0;
        int skippedOrders = 0;
        int skippedItems = 0;

        for (Member member : members) {
            int orderCount = random.nextInt(4); // 0~3건 주문 (랜덤)

            for (int i = 0; i < orderCount; i++) {
                RentalRequest request = new RentalRequest();
                request.setMemberId(member.getId());

                int itemCount = 1 + random.nextInt(4); // 주문당 1~4상품 (랜덤)
                List<RentalRequest.RentalItemRequest> items = new ArrayList<>();

                LocalDateTime orderDate = null;

                for (int j = 0; j < itemCount; j++) {
                    Product product = products.get(random.nextInt(products.size()));
                    int availableStock = product.getAvailableStock();

                    if (availableStock <= 0) {
                        skippedItems++;
                        continue;
                    }

                    int maxQty = Math.min(availableStock, 3);
                    int quantity = 1 + random.nextInt(maxQty);

                    RentalRequest.RentalItemRequest itemReq = new RentalRequest.RentalItemRequest();
                    itemReq.setProductId(product.getId());
                    itemReq.setQuantity(quantity);

                    int dateType = random.nextInt(10);
                    LocalDate rentalStartDate;

                    if (dateType < 3) { // 30% 과거 데이터
                        int yearsAgo = 6 + random.nextInt(4);
                        int daysOffset = random.nextInt(365);
                        rentalStartDate = today.minusYears(yearsAgo).minusDays(daysOffset);
                        itemReq.setPeriodYears(3 + random.nextInt(4));
                    } else { // 70% 최근 데이터
                        int daysAgo = random.nextInt(1095);
                        rentalStartDate = today.minusDays(daysAgo);
                        int[] periods = {3, 4, 5, 6};
                        itemReq.setPeriodYears(periods[random.nextInt(periods.length)]);
                    }

                    itemReq.setRentalStart(rentalStartDate);

                    if (orderDate == null) {
                        int daysBefore = random.nextInt(30);
                        LocalDate orderLocalDate = rentalStartDate.minusDays(daysBefore);
                        int hour = random.nextInt(24);
                        int minute = random.nextInt(60);
                        int second = random.nextInt(60);
                        orderDate = LocalDateTime.of(orderLocalDate, LocalTime.of(hour, minute, second));
                    }

                    items.add(itemReq);
                }

                if (!items.isEmpty()) {
                    request.setItems(items);
                    try {
                        Rental rental = rentalService.createRentalWithDate(request, orderDate);
                        successCount++;

                        // ✅ 새로 생성된 Rental에 포함된 RentalItem을 가져와 구독 생성 + 결제상태 PAID
                        List<RentalItem> rentalItems = rentalItemRepository.findByRental(rental);
                        List<Subscription> subs = rentalItems.stream()
                                .map(item -> Subscription.builder()
                                        .memberId(member.getId())
                                        .rentalItem(item)
                                        .billingKey("TEST-BILLING-" + UUID.randomUUID())
                                        .amount(item.getMonthlyPrice())
                                        .nextBillingDate(item.getRentalStart())
                                        .status(SubscriptionStatus.ACTIVE)
                                        .retryCount(0)
                                        .build())
                                .toList();

                        subscriptionRepository.saveAll(subs);
                        rentalItems.forEach(item -> item.setPaymentStatus(PaymentStatus.PAID));
                        rentalItemRepository.saveAll(rentalItems);

                    } catch (Exception e) {
                        skippedOrders++;
                        System.err.println("주문 생성 실패: " + e.getMessage());
                    }
                } else {
                    skippedOrders++;
                }
            }
        }

        System.out.println("========== 랜덤 렌탈 + 구독 샘플 데이터 생성 완료 ==========");
        System.out.println("✅ 생성된 대여 주문 수: " + successCount);
        System.out.println("⚠️ 재고 부족으로 건너뛴 상품: " + skippedItems);
        System.out.println("❌ 생성 실패한 주문: " + skippedOrders);
    }
}
