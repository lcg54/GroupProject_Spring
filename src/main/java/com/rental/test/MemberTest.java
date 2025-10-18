package com.rental.test;

import com.rental.constant.Role;
import com.rental.entity.Member;
import com.rental.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class MemberTest {
    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void testInsertAndFindMember() {
        Member member = new Member();
        member.setName("홍길동");
        member.setUsername("hong");
        member.setEmail("hong@test.com");
        member.setPassword("1234");
        member.setPhone("010-1111-2222");
        member.setAddress("서울특별시 강남구 테헤란로 123");
        member.setRole(Role.USER);

        Member saved = memberRepository.save(member);

        Member found = memberRepository.findById(saved.getId()).orElse(null);
        System.out.println("조회된 회원: " + found);
    }
}