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

        if (profileImage != null && !profileImage.isEmpty()) {
            String savedName = saveProfileImage(profileImage);
            member.setProfileImage(savedName);
        }

        return memberRepository.save(member);
    }

    public Member login(String username, String password) {
        Member member = memberRepository.findByUsername(username);

        if (member == null) {
            throw new IllegalArgumentException("존재하지 않는 아이디입니다.");
        }

        if (!member.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return member;
    }

    public Member updateMember(MemberRequestDto dto, MultipartFile profileImage) throws IOException {
        Member member = memberRepository.findByUsername(dto.getUsername());

        if (member == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        member.setName(dto.getName());
        member.setEmail(dto.getEmail());
        member.setPhone(dto.getPhone());
        member.setAddress(dto.getAddress());

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            member.setPassword(dto.getPassword());
        }

        if (profileImage != null && !profileImage.isEmpty()) {
            if (member.getProfileImage() != null) {
                deleteOldProfileImage(member.getProfileImage());
            }

            String savedName = saveProfileImage(profileImage);
            member.setProfileImage(savedName);
        }

        return memberRepository.save(member);
    }

    // 회원 탈퇴 메서드 추가
    public void withdrawMember(String username, String password) {
        Member member = memberRepository.findByUsername(username);

        if (member == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // 비밀번호 확인
        if (!member.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 프로필 이미지 삭제
        if (member.getProfileImage() != null) {
            deleteOldProfileImage(member.getProfileImage());
        }

        // 회원 정보 삭제
        memberRepository.delete(member);
    }

    private String saveProfileImage(MultipartFile profileImage) throws IOException {
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

        return savedName;
    }

    private void deleteOldProfileImage(String fileName) {
        try {
            File oldFile = new File(uploadDir, fileName);
            if (oldFile.exists()) {
                oldFile.delete();
            }
        } catch (Exception e) {
            System.err.println("기존 이미지 삭제 실패: " + e.getMessage());
        }
    }
}