package com.ticketbooking.seat.domain.port.in;

import com.ticketbooking.common.dto.SeatAvailabilityDTO;

import java.util.List;

public interface GetAvailableSeatsUseCase {
    List<SeatAvailabilityDTO> getAvailableSeats(Long venueId);
}
