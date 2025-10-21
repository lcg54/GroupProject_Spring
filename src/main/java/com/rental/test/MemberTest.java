package com.rental.test;

import com.rental.constant.Role;
import com.rental.entity.Member;
import com.rental.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MemberTest {
    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void testInsertAndFindMember() {
        Member user01 = new Member();
        user01.setUsername("user01");
        user01.setName("홍길동");
        user01.setEmail("user@example.com");
        user01.setPassword("1234567@");
        user01.setPhone("010-1111-2222");
        user01.setAddress("서울특별시 강남구 테헤란로 123");
        user01.setRole(Role.USER);
        user01.setProfileImage("user_profile.jpg");
        memberRepository.save(user01);

        Member admin01 = new Member();
        admin01.setUsername("admin01");
        admin01.setName("관리자");
        admin01.setEmail("admin@example.com");
        admin01.setPassword("1234567@");
        admin01.setPhone("010-3333-4444");
        admin01.setAddress("서울특별시 마포구 홍익로 10");
        admin01.setRole(Role.ADMIN);
        admin01.setProfileImage("admin_profile.jpg");
        memberRepository.save(admin01);
    }
}