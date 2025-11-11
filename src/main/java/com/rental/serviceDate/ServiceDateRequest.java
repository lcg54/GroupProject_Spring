package com.rental.serviceDate;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDateRequest {
    private Long rentalItemId;     // 프론트에서 보낸 렌탈 아이템 ID
    private LocalDate serviceDate; // 프론트에서 보낸 출장 날짜
    private Long rentalId ; // 프론트에서 보낸 렌탈 ID
}