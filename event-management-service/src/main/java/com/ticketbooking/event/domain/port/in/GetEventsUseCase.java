package com.ticketbooking.event.domain.port.in;

import com.ticketbooking.common.dto.EventDTO;
import java.util.List;

public interface GetEventsUseCase {
    List<EventDTO> getPublishedEvents();
    List<EventDTO> getEventsByCity(String city);
}
