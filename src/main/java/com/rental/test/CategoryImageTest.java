package com.rental.test;

import com.rental.constant.Category;
import com.rental.entity.Product;
import com.rental.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class CategoryImageTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    void injectCategoryImages() {
        // 카테고리 목록
        List<Category> categories = List.of(
                Category.REFRIGERATOR,
                Category.WASHER,
                Category.DRYER,
                Category.AIRCON,
                Category.TV,
                Category.OVEN,
                Category.MICROWAVE,
                Category.OTHER
        );

        for (Category cat : categories) {
            List<Product> products = productRepository.findAll(); // 모든 제품 조회

            for (Product p : products) {
                if (p.getCategory() == cat) {
                    // categoryImage 파일명 규칙: category_카테고리.png
                    String fileName = "category_" + cat.name() + ".png";
                    p.setCategoryImage(fileName);
                    productRepository.save(p);
                    System.out.println("Updated " + p.getName() + " with image: " + fileName);
                }
            }
        }
    }
}