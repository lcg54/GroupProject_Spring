package com.rental.service;

import com.rental.dto.MemberRequestDto;
import com.rental.entity.Member;
import com.rental.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Value("${productImageLocation:C:\\\\shop\\\\images}")
    private String uploadDir;

    public Member registerMember(MemberRequestDto dto, MultipartFile profileImage) throws IOException {
        // 이메일 중복 체크
        if (memberRepository.findByEmail(dto.getEmail()) != null) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Member member = new Member();
        member.setUsername(dto.getUsername());
        member.setName(dto.getName());
        member.setEmail(dto.getEmail());
        member.setPassword(dto.getPassword());
        member.setPhone(dto.getPhone());
        member.setAddress(dto.getAddress());

        // 프로필 이미지 저장
        if (profileImage != null && !profileImage.isEmpty()) {
            String original = profileImage.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String savedName = UUID.randomUUID().toString() + ext;

            File folder = new File(uploadDir);
            if (!folder.exists()) folder.mkdirs();

            File dest = new File(folder, savedName);
            profileImage.transferTo(dest);

            member.setProfileImage(savedName);
        }

        return memberRepository.save(member);
    }

    // 로그인 메서드 추가
    public Member login(String username, String password) {
        Member member = memberRepository.findByUsername(username);

        if (member == null) {
            throw new IllegalArgumentException("존재하지 않는 아이디입니다.");
        }

        // 주의: 실제 운영 환경에서는 BCrypt로 암호화된 비밀번호 비교 필요
        if (!member.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return member;
    }
}