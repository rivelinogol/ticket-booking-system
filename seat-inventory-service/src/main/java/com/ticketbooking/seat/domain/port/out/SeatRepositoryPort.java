package com.ticketbooking.seat.domain.port.out;

import com.ticketbooking.seat.domain.model.Seat;
import com.ticketbooking.seat.domain.model.enums.SeatStatus;

import java.util.List;
import java.util.Optional;

public interface SeatRepositoryPort {
    Seat save(Seat seat);
    Optional<Seat> findById(Long id);
    Optional<Seat> findByIdWithLock(Long id); // pessimistic lock
    List<Seat> findByVenueIdAndStatus(Long venueId, SeatStatus status);
}
