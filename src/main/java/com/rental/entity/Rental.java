package com.rental.entity;

import com.rental.constant.RentalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rentals")
public class Rental {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "rental", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalItem> items = new ArrayList<>();

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private RentalStatus status;

    private LocalDate rentalStart; // 실제 대여 시작일 (예약이면 예정일)
    private LocalDate rentalEnd;   // 대여 종료 예정일/반납일
    // 서비스 출장 날짜 기록용 필드 추가 필요

    private int rentalPeriodYears;  // 대여기간 (3, 4, 5, 6년)
    private int monthlyPrice;       // 선택된 월요금
    private int totalPrice;         // 계산된 총금액 (기간 × 월요금 × 12)

    @PrePersist
    protected void onCreate() { // 주문생성일, 주문상태 RESERVED(예약완료) 자동할당
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = RentalStatus.RESERVED;
    }
}
