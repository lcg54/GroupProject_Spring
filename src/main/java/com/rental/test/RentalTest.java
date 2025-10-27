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
        int skipped = 0;

        for (Member member : members) {
            int orderCount = random.nextInt(4); // 0~3건 주문
            for (int i = 0; i < orderCount; i++) {
                RentalRequest request = new RentalRequest();
                request.setMemberId(member.getId());

                int itemCount = 1 + random.nextInt(3); // 주문당 1~3상품
                List<RentalItemRequest> items = new ArrayList<>();

                for (int j = 0; j < itemCount; j++) {
                    Product product = products.get(random.nextInt(products.size()));

                    // 재고 초과 방지 로직
                    int availableStock = product.getAvailableStock();
                    if (availableStock <= 0) {
                        skipped++;
                        continue; // 대여 가능 재고 없음 → 건너뛰기
                    }
                    int maxQty = Math.min(availableStock, 2); // 상품당 1~2개, 단 재고 초과하지 않게
                    int quantity = 1 + random.nextInt(maxQty); // 1~maxQty 범위 내에서만 주문

                    RentalItemRequest itemReq = new RentalItemRequest();
                    itemReq.setProductId(product.getId());
                    itemReq.setQuantity(quantity);
                    itemReq.setPeriodYears(3 + random.nextInt(4)); // 3~6년
                    itemReq.setRentalStart(today.minusDays(random.nextInt(1000))); // 오늘~1000일 전
                    items.add(itemReq);
                }

                if (!items.isEmpty()) { // 주문할 아이템이 남아 있을 때만 생성
                    request.setItems(items);
                    rentalService.createRental(request);
                }
            }
        }

        System.out.println("✅ 샘플 렌탈 생성 완료");
        System.out.println("⚠️ 재고 부족 상품 수: " + skipped);
    }
}