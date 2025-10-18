package com.rental.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRequestDto {
    private String username;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String address;
}
