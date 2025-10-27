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

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(8); // 상품 등록일
        int totalProducts = 80; // 상품 갯수

        for (int i = 1; i <= totalProducts; i++) {
            Category category = categories[random.nextInt(categories.length)];
            Brand brand = brands[random.nextInt(brands.length)];

            String name = brand.name() + " " + getCategoryName(category) + " " + (100 + i) + " 모델";
            String description = String.format(
                    "%s의 최신 %s 모델입니다. 효율성과 디자인을 모두 잡았습니다. (샘플 %d)",
                    brand.name(), getCategoryName(category), i
            );

            Product p = new Product();
            p.setName(name);
            p.setCategory(category);
            p.setBrand(brand);
            p.setDescription(description);
            p.setPrice(6 * (25 + random.nextInt(25)) * 10000); // 상품 원가: 1,500,000 ~ 3,000,000원 (임시)
            p.setTotalStock(30); // 총 재고 (임시)
            p.setReservedStock(0);
            p.setRentedStock(0);
            p.setRepairStock(0);
            p.setShippingStock(0);
            p.setAvailable(p.getAvailableStock() > 0);

            p.setMainImage(mainImages.get(category));
            p.setCategoryImage("category_" + category.name() + ".png");

            int seq = 1;
            for (String subFileName : subImages.get(category)) {
                ProductImage img = new ProductImage();
                img.setFileName(subFileName);
                img.setSeq(seq++);
                img.setProduct(p);
                p.getImages().add(img);
            }

            double progress = (double) (i - 1) / (totalProducts - 1);
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
            LocalDate regDate = startDate.plusDays((long) (daysBetween * progress));
            p.setRegDate(regDate);

            list.add(p);
        }

        productRepository.saveAll(list);
        injectCategoryImages();
        System.out.println("✅ " + list.size() + "개의 샘플 상품이 성공적으로 추가되었습니다.");
    }

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