package com.rental.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rental_items")
public class RentalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToOne(mappedBy = "rentalItem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Review review;

    @Column(nullable = false)
    private int quantity; // 수량

    private int pricePerUnit; // 상품별 고정 월 단가 (구매시점에 고정되어 상품가격이 수정되어도 바뀌지 않도록 따로 설정)

    private int rentalPeriodYears; // 대여 기간 (3,4,5,6년)
    private LocalDate rentalStart; // 대여 시작일
    private LocalDate rentalEnd;   // 대여 종료일

    // private LocalDate serviceDate; // 서비스 출장일
}