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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private String adminName; // 누가

    @Column(nullable = false)
    private LocalDateTime createdAt; // 언제

    @Column(name = "log_event", nullable = false, length = 10) // 수정(삭제), 등록 내역
    private String event;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}