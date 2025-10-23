package com.rental.test;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.entity.Product;
import com.rental.entity.ProductImage;
import com.rental.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.*;

@SpringBootTest
public class ProductTest {
    @Autowired
    private ProductRepository productRepository;

    private static final Random random = new Random();

    @Test
    void insertBulkSampleProductsAndInjectCategoryImages() {
        long existing = productRepository.count();
        if (existing > 0) {
            System.out.println("이미 상품이 존재하므로 샘플 추가를 생략합니다. (현재 " + existing + "개)");
            injectCategoryImages();
            return;
        }

        List<Product> list = new ArrayList<>();
        Category[] categories = Category.values();
        Brand[] brands = Brand.values();

        // 상품이미지 세팅 (카테고리별 통일)
        Map<Category, String> mainImages = new HashMap<>();
        Map<Category, List<String>> subImages = new HashMap<>();
        for (Category cat : categories) {
            mainImages.put(cat, "main_" + cat.name() + ".avif");
            List<String> subs = new ArrayList<>();
            for (int j = 1; j <= 5; j++) {
                subs.add("sub_" + cat.name() + "_" + j + ".avif");
            }
            subImages.put(cat, subs);
        }

        // 등록일 기준 설정
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(8); // 8년 전부터 시작
        int totalProducts = 120;

        for (int i = 1; i <= totalProducts; i++) {
            Category category = categories[random.nextInt(categories.length)];
            Brand brand = brands[random.nextInt(brands.length)];

            String name = brand.name() + " " + getCategoryName(category) + " " + (100 + i) + " 모델";
            String description = String.format(
                    "%s의 최신 %s 모델입니다. 효율성과 디자인을 모두 잡았습니다. (샘플 %d)",
                    brand.name(), getCategoryName(category), i
            );

            // 가격: 1,500,000 ~ 3,000,000 (랜덤)
            int pricePerPeriod = 6 * (25 + random.nextInt(25)) * 10000;

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

            // 메인 이미지
            p.setMainImage(mainImages.get(category));

            // 카테고리 이미지
            p.setCategoryImage("category_" + category.name() + ".png");

            // 서브 이미지
            int seq = 1;
            for (String subFileName : subImages.get(category)) {
                ProductImage img = new ProductImage();
                img.setFileName(subFileName);
                img.setSeq(seq++);
                img.setProduct(p);
                p.getImages().add(img);
            }

            // ✅ 등록일: 8년 전 ~ 오늘 사이, 순차적으로 증가
            // 오래된 상품부터 최신 상품 순으로 날짜 증가
            double progress = (double) (i - 1) / (totalProducts - 1);
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
            LocalDate regDate = startDate.plusDays((long) (daysBetween * progress));
            p.setRegDate(regDate);

            list.add(p);
        }

        productRepository.saveAll(list);
        System.out.println("✅ " + list.size() + "개의 샘플 상품이 성공적으로 추가되었습니다.");

        // 마지막으로 categoryImage 일괄 검증
        injectCategoryImages();
    }

    // 이미 존재하는 모든 상품에 대해 categoryImage 필드 주입
    private void injectCategoryImages() {
        List<Product> all = productRepository.findAll();
        int count = 0;
        for (Product p : all) {
            if (p.getCategoryImage() == null || p.getCategoryImage().isEmpty()) {
                String fileName = "category_" + p.getCategory().name() + ".png";
                p.setCategoryImage(fileName);
                count++;
            }
        }
        productRepository.saveAll(all);
        System.out.println("📦 " + count + "개의 상품에 categoryImage가 추가 주입되었습니다.");
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