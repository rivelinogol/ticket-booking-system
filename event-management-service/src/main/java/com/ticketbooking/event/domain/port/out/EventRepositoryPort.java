package com.ticketbooking.event.domain.port.out;

import com.ticketbooking.event.domain.model.Event;
import com.ticketbooking.event.domain.model.enums.EventStatus;
import java.util.List;
import java.util.Optional;

public interface EventRepositoryPort {
    Event save(Event event);
    Optional<Event> findById(Long id);
    List<Event> findByStatus(EventStatus status);
    List<Event> findByVenueCityAndStatus(String city, EventStatus status);
}
