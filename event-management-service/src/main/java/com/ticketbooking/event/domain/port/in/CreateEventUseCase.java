package com.ticketbooking.event.domain.port.in;

import com.ticketbooking.common.dto.EventDTO;

public interface CreateEventUseCase {
    EventDTO createEvent(EventDTO request);
}
