package com.rental.controller;

import com.rental.dto.MemberRequestDto;
import com.rental.entity.Member;
import com.rental.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class MemberController {

    private final MemberService memberService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> registerMember(
            @ModelAttribute MemberRequestDto memberRequestDto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            Member saved = memberService.registerMember(memberRequestDto, profileImage);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("회원가입 실패: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

    // 로그인 엔드포인트 추가
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        try {
            Member member = memberService.login(username, password);
            return ResponseEntity.ok(member);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

    record ErrorResponse(String message) {}
}