package com.rental.test;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import com.rental.product.Product;
import com.rental.product.ProductImage;
import com.rental.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

@SpringBootTest
public class ProductTest {
    @Autowired
    private ProductRepository productRepository;

    private static final Random random = new Random();

    @Test
    void insertBulkSampleProductsWithSufficientStock() {
        long existing = productRepository.count();
        if (existing > 0) {
            System.out.println("이미 상품이 존재하므로 샘플 추가를 생략합니다. (현재 " + existing + "개)");
            return;
        }

        String rootPath = "C:/shop/images/category/";
        List<Product> allProducts = new ArrayList<>();
        Category[] categories = Category.values();
        Brand[] brands = Brand.values();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusYears(15);

        for (Category category : categories) {
            File categoryDir = new File(rootPath + category.name());

            System.out.println(categoryDir.getAbsolutePath() + " : " + categoryDir.exists());

            if (!categoryDir.exists() || !categoryDir.isDirectory()) continue;

            File[] productFolders = categoryDir.listFiles(File::isDirectory);
            if (productFolders == null || productFolders.length == 0) continue;

            for (File productFolder : productFolders) {
                String folderName = productFolder.getName();
                String productName = folderName.contains("_") ? folderName.split("_")[0] : folderName;

                Product p = new Product();
                p.setName(productName);
                p.setCategory(category);
                p.setBrand(brands[random.nextInt(brands.length)]);
                p.setDescription(productName + " 샘플 설명입니다.");
                p.setPrice(6 * (25 + random.nextInt(25)) * 10000);
                p.setTotalStock(999); // 총 재고 (임시)
                p.setReservedStock(0);
                p.setRentedStock(0);
                p.setRepairStock(0);
                p.setShippingStock(0);
                p.setAvailable(true);
                p.setCategoryImage("category_" + category.name() + ".png");

                List<ProductImage> images = new ArrayList<>();
                int seq = 1;
                File[] imageFiles = productFolder.listFiles((dir, name) -> name.matches("(?i).+\\.(jpg|jpeg|png|gif|avif)$"));
                if (imageFiles != null) {
                    Arrays.sort(imageFiles, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
                    for (File f : imageFiles) {
                        String path = rootPath + category.name() + "/" + folderName + "/" + f.getName();
                        ProductImage img = new ProductImage();
                        img.setFileName(path);
                        img.setSeq(seq++);
                        img.setProduct(p);
                        images.add(img);

                        if (p.getMainImage() == null && f.getName().toLowerCase().startsWith("main_")) {
                            p.setMainImage(path);
                        }
                    }
                }

                if (p.getMainImage() == null && !images.isEmpty()) {
                    p.setMainImage(images.get(0).getFileName());
                }

                p.setImages(images);

                double progress = random.nextDouble();
                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, today);
                LocalDate regDate = startDate.plusDays((long) (daysBetween * progress));
                p.setRegDate(regDate);

                allProducts.add(p);
            }
        }

        if (!allProducts.isEmpty()) {
            productRepository.saveAll(allProducts);
            System.out.println("✅ " + allProducts.size() + "개의 샘플 상품 생성 완료");
        } else {
            System.out.println("⚠️ 카테고리 폴더에서 상품을 찾지 못했습니다.");
        }
    }
}