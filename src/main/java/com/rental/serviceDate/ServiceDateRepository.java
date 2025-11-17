package com.rental.serviceDate;

import com.rental.rental.RentalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceDateRepository extends JpaRepository<ServiceDate, Long> {
    // 특정 렌탈 아이템과 서비스 날짜에 맞는 데이터를 조회
    Optional<ServiceDate> findByRentalItemAndServiceDate(RentalItem rentalItem, LocalDate serviceDate);
    List<ServiceDate> findByRentalItem_Id(Long rentalItemId);

    // 관리자가 특정 날짜에 예약된 서비스 확인
    List<ServiceDate> findByServiceDate(LocalDate date);
}