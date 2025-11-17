package com.rental.wishlist;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DeleteSelectedRequest {
    @NotNull
    private Long memberId;

    @NotEmpty
    private List<Long> productIds;
}