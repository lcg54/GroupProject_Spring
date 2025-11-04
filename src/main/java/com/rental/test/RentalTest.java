package com.rental.test;

import com.rental.dto.RentalRequest;
import com.rental.dto.RentalRequest.RentalItemRequest;
import com.rental.entity.Member;
import com.rental.entity.Product;
import com.rental.service.RentalService;
import com.rental.repository.MemberRepository;
import com.rental.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class RentalTest {

    @Autowired
    private RentalService rentalService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ProductRepository productRepository;

    private static final Random random = new Random();

    @Test
    void insertSampleRentals() {
        List<Member> members = memberRepository.findAllByIdBetween(2L, 301L); // 일반회원 전체
        List<Product> products = productRepository.findAll();

        LocalDate today = LocalDate.now();
        int successCount = 0;
        int skippedOrders = 0;
        int skippedItems = 0;

        for (Member member : members) {
            int orderCount = random.nextInt(6); // 0~5건 주문 (랜덤)

            for (int i = 0; i < orderCount; i++) {
                RentalRequest request = new RentalRequest();
                request.setMemberId(member.getId());

                int itemCount = 1 + random.nextInt(4); // 주문당 1~4상품 (랜덤)
                List<RentalItemRequest> items = new ArrayList<>();

                // 주문일(createdAt)을 랜덤하게 설정
                LocalDateTime orderDate = null;

                for (int j = 0; j < itemCount; j++) {
                    Product product = products.get(random.nextInt(products.size()));

                    // 재고 초과 방지 로직
                    int availableStock = product.getAvailableStock();
                    if (availableStock <= 0) {
                        skippedItems++;
                        continue; // 대여 가능 재고 없음 → 건너뛰기
                    }

                    int maxQty = Math.min(availableStock, 3); // 상품당 최대 3개, 단 재고 초과하지 않게
                    int quantity = 1 + random.nextInt(maxQty); // 1~maxQty 범위 내에서만 주문

                    RentalItemRequest itemReq = new RentalItemRequest();
                    itemReq.setProductId(product.getId());
                    itemReq.setQuantity(quantity);

                    // 날짜 랜덤 분배: 과거 데이터 30%, 최근 데이터 70%
                    int dateType = random.nextInt(10);
                    LocalDate rentalStartDate;

                    if (dateType < 3) { // 30% - 6~9년 전 데이터 (반납 완료)
                        int yearsAgo = 6 + random.nextInt(4); // 6~9년 전
                        int daysOffset = random.nextInt(365); // 해당 연도 내 랜덤 날짜
                        rentalStartDate = today.minusYears(yearsAgo).minusDays(daysOffset);
                        itemReq.setPeriodYears(3 + random.nextInt(4)); // 3~6년

                    } else { // 70% - 최근 3년 이내 데이터
                        int daysAgo = random.nextInt(1095); // 0~1095일 전 (약 3년)
                        rentalStartDate = today.minusDays(daysAgo);

                        // 대여 기간도 랜덤하게
                        int[] periods = {3, 4, 5, 6};
                        itemReq.setPeriodYears(periods[random.nextInt(periods.length)]);
                    }

                    itemReq.setRentalStart(rentalStartDate);

                    // 주문일(createdAt)은 대여 시작일과 비슷하거나 조금 이전으로 설정
                    // 첫 번째 아이템에서 결정된 주문일을 모든 아이템이 공유
                    if (orderDate == null) {
                        int daysBefore = random.nextInt(30); // 대여 시작일 0~30일 전에 주문
                        LocalDate orderLocalDate = rentalStartDate.minusDays(daysBefore);

                        // 랜덤한 시간 추가 (00:00:00 ~ 23:59:59)
                        int hour = random.nextInt(24);
                        int minute = random.nextInt(60);
                        int second = random.nextInt(60);
                        orderDate = LocalDateTime.of(orderLocalDate, LocalTime.of(hour, minute, second));
                    }

                    items.add(itemReq);
                }

                if (!items.isEmpty()) { // 주문할 아이템이 남아 있을 때만 생성
                    request.setItems(items);
                    try {
                        // createRentalWithDate 메서드 사용 (주문일 지정)
                        rentalService.createRentalWithDate(request, orderDate);
                        successCount++;
                    } catch (Exception e) {
                        skippedOrders++;
                        System.err.println("주문 생성 실패: " + e.getMessage());
                    }
                } else {
                    skippedOrders++;
                }
            }
        }

        System.out.println("========== 랜덤 렌탈 생성 완료 ==========");
        System.out.println("✅ 성공한 주문 수: " + successCount);
        System.out.println("⚠️ 재고 부족으로 건너뛴 상품: " + skippedItems);
        System.out.println("❌ 생성 실패한 주문: " + skippedOrders);
    }
}