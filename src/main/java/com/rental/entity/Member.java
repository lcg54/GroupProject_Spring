package com.rental.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rental.constant.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String username; // 화면에 표시될 고유 아이디(회원이 지정) - PK 아님

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;


    @Column(nullable = false)
    @Pattern(regexp = ".*[!@#$%].*", message = "비밀 번호는 특수 문자 '!@#$%' 중 하나 이상을 포함해야 합니다.")
    private String password;

    @Column(unique = true)
    private String phone;

    private String address;

    private String profileImage; // 서버 저장 이미지(파일명)

    @Enumerated(EnumType.STRING)
    private Role role;

    @JsonFormat(pattern = "yyy-MM-dd")
    private LocalDate regDate;

    @PrePersist // 생성시 role 지정을 안하면 role=USER 자동할당, regDate=생성일 자동할당
    protected void onCreate() {
        if (this.role == null) this.role = Role.USER;
        this.regDate = LocalDate.now();
    }

}
