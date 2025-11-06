package com.rental.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "product_log")
public class ProductLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "admin_id")
    private Member member;

    @Column(nullable = false)
    private LocalDateTime createdAt; // 언제

    @Column(name = "log_event", nullable = false, length = 10) // 수정(삭제), 등록 내역
    private String event;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}