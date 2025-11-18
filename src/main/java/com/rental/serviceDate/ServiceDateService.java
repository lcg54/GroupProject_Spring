package com.rental.serviceDate;

import com.rental.rental.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceDateService {

    private final ServiceDateRepository serviceDateRepository;
    private final RentalItemRepository rentalItemRepository;
    private final RentalRepository rentalRepository;

    @Transactional
    public ServiceDate addServiceDate(Long rentalId, Long rentalItemId, LocalDate date) {
        // Rental 조회
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("해당 렌탈이 존재하지 않습니다."));

        // RentalItem 조회
        RentalItem rentalItem = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 렌탈 아이템이 존재하지 않습니다."));

        // 중복 체크
        if (serviceDateRepository.findByRentalItemAndServiceDate(rentalItem, date).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 서비스 날짜입니다.");
        }

        ServiceDate serviceDate = new ServiceDate();
        serviceDate.setRental(rental);
        serviceDate.setRentalItem(rentalItem);
        serviceDate.setServiceDate(date);

        return serviceDateRepository.save(serviceDate);
    }

    @Transactional
    public boolean removeServiceDate(Long rentalItemId, LocalDate date) {
        RentalItem rentalItem = rentalItemRepository.findById(rentalItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 렌탈 아이템이 존재하지 않습니다."));

        return serviceDateRepository.findByRentalItemAndServiceDate(rentalItem, date)
                .map(sd -> {
                    serviceDateRepository.delete(sd);
                    return true;
                }).orElse(false);
    }

    // 전체 서비스일 목록
    @Transactional(readOnly = true)
    public List<ServiceDateAdminDto> getAllServiceDates() {
        List<ServiceDate> all = serviceDateRepository.findAll();
        return all.stream().map(sd -> new ServiceDateAdminDto(
                sd.getId(),
                sd.getServiceDate(),
                sd.getRental().getId(),
                sd.getRentalItem().getId(),
                sd.getRentalItem().getProduct().getName(),
                sd.getRental().getMember().getId(),
                sd.getRental().getMember().getName(),
                sd.getRental().getMember().getAddress(),
                sd.getRentalItem().getRentalStart(),
                sd.getRentalItem().getRentalEnd()
        )).toList();
    }

    // 특정 날짜 서비스 조회
    @Transactional(readOnly = true)
    public List<ServiceDateAdminDto> getServiceDatesByDate(LocalDate date) {
        List<ServiceDate> list = serviceDateRepository.findByServiceDate(date);
        return list.stream().map(sd -> new ServiceDateAdminDto(
                sd.getId(),
                sd.getServiceDate(),
                sd.getRental().getId(),
                sd.getRentalItem().getId(),
                sd.getRentalItem().getProduct().getName(),
                sd.getRental().getMember().getId(),
                sd.getRental().getMember().getName(),
                sd.getRental().getMember().getAddress(),
                sd.getRentalItem().getRentalStart(),
                sd.getRentalItem().getRentalEnd()
        )).toList();
    }
}