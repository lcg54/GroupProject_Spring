package com.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDateRequest {
    private Long rentalItemId;     // 프론트에서 보낸 렌탈 아이템 ID
    private LocalDate serviceDate; // 프론트에서 보낸 출장 날짜


}