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
    private int reservedStock; // 예약(미확정/예약중) 수량

    @Column(nullable = false)
    private int rentedStock; // 현재 대여중인 수량

    @Column(nullable = false)
    private int repairStock; // 수리중인 수량

    @Column(nullable = false)
    private String mainImage; // 대표 이미지 파일명

    @Column(length = 2000)
    private String description;

    private LocalDate regDate;

    @PrePersist
    protected void onCreate() {
        this.regDate = LocalDate.now();
    }

    public int getAvailableStock() { // 대여가능재고
        int unavailable = reservedStock + rentedStock + repairStock;
        return Math.max(totalStock - unavailable, 0);
    }
}