package com.rental.service;

import com.rental.dto.RentalResponse;
import com.rental.entity.Rental;
import com.rental.entity.RentalItem;
import com.rental.entity.ServiceDate;
import com.rental.repository.RentalItemRepository;
import com.rental.repository.RentalRepository;
import com.rental.repository.ServiceDateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ServiceDateService {

    private final ServiceDateRepository serviceDateRepository;
    private final RentalItemRepository rentalItemRepository;
    private final RentalRepository rentalRepository;

    public ServiceDateService(ServiceDateRepository serviceDateRepository, RentalItemRepository rentalItemRepository, RentalResponse rentalResponse, RentalRepository rentalRepository) {
        this.serviceDateRepository = serviceDateRepository;
        this.rentalItemRepository = rentalItemRepository;
        this.rentalRepository = rentalRepository;
    }

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
