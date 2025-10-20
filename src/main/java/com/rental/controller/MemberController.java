package com.rental.controller;

import com.rental.entity.Member;
import com.rental.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class MemberController {
    private final MemberRepository memberRepository;

    @PostMapping("/test")
    public ResponseEntity<?> testLogin() {
        Optional<Member> memberOpt = memberRepository.findById(2L);
        if (memberOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("테스트용 유저를 찾을 수 없습니다.");
        }
        Member member = memberOpt.get();
        return ResponseEntity.ok(member);
    }
}
