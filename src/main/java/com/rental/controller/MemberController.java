package com.rental.controller;

import com.rental.dto.MemberRequestDto;
import com.rental.entity.Member;
import com.rental.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> registerMember(
            @ModelAttribute MemberRequestDto memberRequestDto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            Member saved = memberService.registerMember(memberRequestDto, profileImage);

            // 비밀번호 제거 후 반환
            Map<String, Object> response = createMemberResponse(saved);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("회원가입 실패: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        try {
            Member member = memberService.login(username, password);

            // 비밀번호 제거 후 반환
            Map<String, Object> response = createMemberResponse(member);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

    @PutMapping(value = "/edit", consumes = {"multipart/form-data"})
    public ResponseEntity<?> editMember(
            @ModelAttribute MemberRequestDto memberRequestDto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            Member updated = memberService.updateMember(memberRequestDto, profileImage);

            // 비밀번호 제거 후 반환
            Map<String, Object> response = createMemberResponse(updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdrawMember(@RequestBody Map<String, String> withdrawData) {
        String username = withdrawData.get("username");
        String password = withdrawData.get("password");

        try {
            memberService.withdrawMember(username, password);
            return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

    // 비밀번호를 제외한 회원 정보 생성
    private Map<String, Object> createMemberResponse(Member member) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", member.getId());
        response.put("username", member.getUsername());
        response.put("name", member.getName());
        response.put("email", member.getEmail());
        response.put("phone", member.getPhone());
        response.put("address", member.getAddress());
        response.put("profileImage", member.getProfileImage());
        response.put("role", member.getRole());
        response.put("regDate", member.getRegDate());
        return response;
    }

    record ErrorResponse(String message) {}
}