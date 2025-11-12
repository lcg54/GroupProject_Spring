package com.rental.serviceDate;

import com.rental.rental.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
}