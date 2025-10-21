package com.rental.test;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.entity.Product;
import com.rental.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@SpringBootTest
public class ProductTest {
    @Autowired
    private ProductRepository productRepository;

    private static final Random random = new Random();

    @Test
    void insertBulkSampleProducts() {
        long existing = productRepository.count();
        if (existing > 0) {
            System.out.println("이미 상품이 존재하므로 샘플 추가를 생략합니다. (현재 " + existing + "개)");
            return;
        }

        List<Product> list = new ArrayList<>();

        Category[] categories = Category.values();
        Brand[] brands = Brand.values();

        for (int i = 1; i <= 120; i++) {
            Category category = categories[random.nextInt(categories.length)];
            Brand brand = brands[random.nextInt(brands.length)];

            String name = brand.name() + " " + getCategoryName(category) + " " + (100 + i) + " 모델";
            String description = String.format(
                    "%s의 최신 %s 모델입니다. 효율성과 디자인을 모두 잡았습니다. (샘플 %d)",
                    brand.name(), getCategoryName(category), i
            );

            // 가격: 900,000 ~ 3,000,000 (랜덤)
            int pricePerPeriod = 6 * (15 + random.nextInt(35)) * 10000;

            // 기본 재고
            int totalStock = 20 + random.nextInt(10);
            int reservedStock = random.nextInt(5);
            int rentedStock = random.nextInt(10);
            int repairStock = random.nextInt(2);

            // 약 12%의 상품은 대여불가 상태로 만들기
            boolean makeUnavailable = random.nextDouble() < 0.12;
            if (makeUnavailable) {
                reservedStock = totalStock / 3;
                rentedStock = totalStock / 2;
                repairStock = totalStock - (reservedStock + rentedStock);
            }

            Product p = new Product();
            p.setName(name);
            p.setCategory(category);
            p.setBrand(brand);
            p.setDescription(description);
            p.setPrice(pricePerPeriod);
            p.setTotalStock(totalStock);
            p.setReservedStock(reservedStock);
            p.setRentedStock(rentedStock);
            p.setRepairStock(repairStock);
            p.setMainImage("sample_" + UUID.randomUUID() + ".jpg");

            // 리뷰 리스트는 자동 초기화되어 있음 (@OneToMany 초기값 new ArrayList<>)
            // 별도 setReviews() 호출 필요 없음

            list.add(p);
        }

        productRepository.saveAll(list);
        System.out.println("✅ " + list.size() + "개의 샘플 상품이 성공적으로 추가되었습니다.");
    }

    private String getCategoryName(Category category) {
        return switch (category) {
            case REFRIGERATOR -> "냉장고";
            case WASHER -> "세탁기";
            case DRYER -> "건조기";
            case AIRCON -> "에어컨";
            case TV -> "TV";
            case OVEN -> "오븐";
            case MICROWAVE -> "전자레인지";
            case OTHER -> "기타가전";
        };
    }
}