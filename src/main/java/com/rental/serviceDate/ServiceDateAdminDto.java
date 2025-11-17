package com.rental.serviceDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class ServiceDateAdminDto {
    private Long serviceDateId;
    private LocalDate serviceDate;
    private Long rentalId;
    private Long rentalItemId;
    private String productName;
    private Long memberId;
    private String memberName;
    private LocalDate rentalStart;
    private LocalDate rentalEnd;
}
