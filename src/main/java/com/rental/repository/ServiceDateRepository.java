package com.rental.repository;

import com.rental.entity.RentalItem;
import com.rental.entity.ServiceDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceDateRepository extends JpaRepository<ServiceDate, Long> {

    // 특정 렌탈 아이템에 대한 서비스 날짜들 조회
    List<ServiceDate> findByRentalItem(RentalItem rentalItem);

    // 특정 렌탈 아이템과 서비스 날짜에 맞는 데이터를 조회
    Optional<ServiceDate> findByRentalItemAndServiceDate(RentalItem rentalItem, LocalDate serviceDate);
}
