package com.ticketbooking.event.domain.port.in;

import com.ticketbooking.common.dto.EventDTO;
import com.ticketbooking.common.dto.SeatAvailabilityDTO;
import java.util.List;

public interface GetEventDetailsUseCase {
    EventDTO getEventById(Long eventId);
    List<SeatAvailabilityDTO> getSeatAvailability(Long eventId);
}
