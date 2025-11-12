package com.rental.wishlist;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WishListRequest {
    @NotNull private Long memberId;
    @NotNull private Long productId;
}