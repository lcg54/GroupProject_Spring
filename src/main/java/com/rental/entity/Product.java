package com.rental.entity;

import com.rental.constant.Brand;
import com.rental.constant.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @PrePersist  // 생성시 regDate=생성일 자동할당
    protected void onCreate() {
        this.regDate = LocalDate.now();
    }

    public int getAvailableStock() { // 사용 가능 재고 계산용 메서드
        int unavailable = (reservedStock) + (rentedStock) + (repairStock);
        int available = totalStock - unavailable;
        return Math.max(available, 0);
    }
}
