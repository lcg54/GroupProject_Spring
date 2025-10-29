package com.rental.entity;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private Brand brand;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private int price; // 기본 단가

    @Column(nullable = false)
    private int totalStock; // 총 보유 수량
    @Column(nullable = false)
    private int reservedStock; // 예약 수량
    @Column(nullable = false)
    private int shippingStock; // 배송 중인 수량
    @Column(nullable = false)
    private int rentedStock; // 대여 중인 수량
    @Column(nullable = false)
    private int repairStock; // 수리 중인 수량
    @Column(nullable = false)
    private int returnRequestedStock; // 반납 요청 중인 수량

    @Column(nullable = false)
    private String mainImage; // 대표 이미지 파일명

    private String categoryImage; // 카테고리 이미지

    @Column(length = 2000)
    private String description;

    private Boolean available; // 재고 사용 가능 여부

    private LocalDate regDate;

    @PrePersist
    protected void onCreate() {
        this.available = true;
    }

    public int getAvailableStock() { // 대여 가능 재고
        int unavailableStock = reservedStock + shippingStock + rentedStock + repairStock + returnRequestedStock;
        return Math.max(totalStock - unavailableStock, 0);
    }
}