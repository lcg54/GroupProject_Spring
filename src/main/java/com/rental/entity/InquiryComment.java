package com.rental.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inquiry_comments")
public class InquiryComment { // 문의글의 답변글 (관리자만 1회)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id")
    private Inquiry inquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Member admin; // 관리자 작성자 (role==ADMIN)

    @Column(length = 4000)
    private String comment;

    private LocalDateTime createdAt;

    @PrePersist // 생성일 자동할당
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
