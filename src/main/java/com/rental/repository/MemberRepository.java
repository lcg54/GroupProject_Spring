package com.rental.repository;

import com.rental.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 회원 정보를 관리하는 Repository
 * JPA를 통해 데이터베이스와 통신
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일로 회원 조회
     * @param email 조회할 이메일
     * @return 회원 엔티티
     */
    Member findByEmail(String email);

    /**
     * 사용자명으로 회원 조회
     * @param username 조회할 사용자명
     * @return 회원 엔티티
     */
    Member findByUsername(String username);

    /**
     * 사용자명으로 회원 존재 여부 확인
     * @param username 확인할 사용자명
     * @return 존재 여부
     */
    boolean existsByUsername(String username);

    /**
     * 이메일로 회원 존재 여부 확인
     * @param email 확인할 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * ID로 회원 조회 (Optional 반환)
     * @param id 회원 ID
     * @return Optional<Member>
     */
    @Query("SELECT m FROM Member m WHERE m.id = :id")
    Optional<Member> findMemberById(@Param("id") Long id);

    List<Member> findAllByIdBetween(long startId, long endId);
}