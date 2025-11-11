package com.rental.serviceDate;

import com.rental.rental.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ServiceDateService {

    private final ServiceDateRepository serviceDateRepository;
    private final RentalItemRepository rentalItemRepository;
    private final RentalRepository rentalRepository;

//    public List<LocalDate> getServiceDatesByRentalId(Long rentalItemId) {
//        RentalItem rentalItem = rentalItemRepository.findById(rentalItemId)
//                .orElseThrow(() -> new IllegalArgumentException("해당 렌탈 아이템이 존재하지 않습니다."));
//        return serviceDateRepository.findByRentalItem(rentalItem)
//                .stream()
//                .map(ServiceDate::getServiceDate)
//                .toList();
//    }

    public ServiceDate addServiceDate(Long rentalId, LocalDate date) {
        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("해당 렌탈이 존재하지 않습니다."));

        ServiceDate serviceDate = new ServiceDate();
        serviceDate.setRental(rental);  // 여기서 Rental로 연결
        serviceDate.setServiceDate(date);
        return serviceDateRepository.save(serviceDate);
    }

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