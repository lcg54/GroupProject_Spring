package com.rental.test;

import com.rental.constant.Role;
import com.rental.entity.Member;
import com.rental.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class MemberTest {
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Random random = new Random();

    @Test
    public void insertSampleMembers() {
        long existing = memberRepository.count();
        if (existing > 0) {
            System.out.println("✅ 이미 회원 데이터가 존재합니다. (" + existing + "명)");
            return;
        }

        List<Member> list = new ArrayList<>();

        // ✅ 관리자 1명
        Member admin = new Member();
        admin.setUsername("admin01");
        admin.setName("관리자");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("1234567@"));
        admin.setPhone("010-9999-0000");
        admin.setAddress("서울특별시 마포구 홍익로 10");
        admin.setRole(Role.ADMIN);
        admin.setProfileImage("admin_profile.jpg");
        list.add(admin);

        // ✅ 일반 회원 100명
        for (int i = 1; i <= 100; i++) {
            Member user = new Member();
            user.setUsername("user" + String.format("%03d", i));
            user.setName(getRandomKoreanName());
            user.setEmail("user" + i + "@example.com");
            user.setPassword(passwordEncoder.encode("1234567@"));
            user.setPhone("010-" + (1000 + i) + "-" + (2000 + i));
            user.setAddress(getRandomAddress());
            user.setRole(Role.USER);
            user.setProfileImage("user_profile_" + ((i % 5) + 1) + ".jpg"); // 5종류 프로필
            list.add(user);
        }

        memberRepository.saveAll(list);
        System.out.println("✅ 회원 데이터 생성 완료: 관리자 1명 + 일반회원 100명 (" + list.size() + "명)");
    }

    // 랜덤 한국 이름 생성
    private String getRandomKoreanName() {
        String[] lastNames = {"김", "이", "박", "최", "정", "조", "윤", "임", "장", "한"};
        String[] firstNames = {"민수", "지현", "서준", "예린", "도윤", "하은", "지호", "유진", "현우", "지민"};
        return lastNames[random.nextInt(lastNames.length)] + firstNames[random.nextInt(firstNames.length)];
    }

    // 랜덤 주소 생성
    private String getRandomAddress() {
        String[] cities = {"서울특별시", "부산광역시", "대구광역시", "인천광역시", "광주광역시", "대전광역시", "울산광역시"};
        String[] districts = {"강남구", "서초구", "마포구", "송파구", "수성구", "남구", "중구", "동구", "서구"};
        return cities[random.nextInt(cities.length)] + " " + districts[random.nextInt(districts.length)] + " " + (10 + random.nextInt(90)) + "길";
    }
}