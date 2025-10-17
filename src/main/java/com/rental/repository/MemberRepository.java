package com.rental.repository;

import com.rental.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    // 이메일 정보를 이용하여 해당 회원이 존재하는 지 체크
    Member findByEmail(String email);
}
