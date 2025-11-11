package com.rental.wishlist;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
  name = "wishlist",
  uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "product_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishList {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Setter
  @Column(name = "member_id", nullable = false)
  private Long memberId; // 회원 ID

  @Setter
  @Column(name = "product_id", nullable = false)
  private Long productId; // 상품 ID

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt; // 생성 시각


  public WishList(Long memberId, Long productId) {
    this.memberId = memberId;
    this.productId = productId;
  }

  @PrePersist
  void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = LocalDateTime.now();
    }
  }
}