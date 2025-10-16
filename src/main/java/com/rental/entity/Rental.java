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

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private RentalStatus status;

    private LocalDate rentalStart; // 실제 대여 시작일 (예약이면 예정일)
    private LocalDate rentalEnd;   // 대여 종료 예정일/반납일

    private int totalPrice; // 계산된 총 금액

    @OneToMany(mappedBy = "rental", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() { // 생성시 createdAt=주문일 자동할당, status 지정을 안하면 status=RESERVED(예약완료) 자동할당
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = RentalStatus.RESERVED;
    }
}
