package com.rental.serviceDate;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ServiceDateDto {
    private Long rentalItemId;
    private LocalDate serviceDate;
}