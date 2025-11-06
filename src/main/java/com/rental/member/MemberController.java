package com.rental.member;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
            @ModelAttribute MemberRequest memberRequest,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            Member saved = memberService.registerMember(memberRequest, profileImage);
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
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData, HttpSession session) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        try {
            Member member = memberService.login(username, password);

            // 세션에 사용자 정보 저장 (중요!)
            Map<String, Object> userData = createMemberResponse(member);
            session.setAttribute("user", userData);
            session.setAttribute("userId", member.getId());
            session.setAttribute("username", member.getUsername());
            session.setAttribute("role", member.getRole().toString());

            System.out.println("로그인 성공 - 세션 ID: " + session.getId());
            System.out.println("저장된 사용자 정보: " + userData);

            return ResponseEntity.ok(userData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

    // 세션 확인 엔드포인트 (중요!)
    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(HttpSession session) {
        Object user = session.getAttribute("user");

        if (user != null) {
            System.out.println("세션 확인 성공 - 세션 ID: " + session.getId());
            System.out.println("세션 사용자 정보: " + user);
            return ResponseEntity.ok(user);
        }

        System.out.println("세션 없음 - 세션 ID: " + session.getId());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("세션이 없습니다."));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        try {
            System.out.println("로그아웃 - 세션 ID: " + session.getId());
            session.invalidate(); // 세션 무효화
            return ResponseEntity.ok(Map.of("message", "로그아웃 되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("로그아웃 중 오류가 발생했습니다."));
        }
    }

    @PutMapping(value = "/edit", consumes = {"multipart/form-data"})
    public ResponseEntity<?> editMember(
            @ModelAttribute MemberRequest memberRequest,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            HttpSession session) {
        try {
            Member updated = memberService.updateMember(memberRequest, profileImage);

            // 세션 업데이트
            Map<String, Object> response = createMemberResponse(updated);
            session.setAttribute("user", response);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdrawMember(@RequestBody Map<String, String> withdrawData, HttpSession session) {
        String username = withdrawData.get("username");
        String password = withdrawData.get("password");

        try {
            memberService.withdrawMember(username, password);
            session.invalidate(); // 탈퇴 시 세션 무효화
            return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("서버 오류가 발생했습니다."));
        }
    }

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