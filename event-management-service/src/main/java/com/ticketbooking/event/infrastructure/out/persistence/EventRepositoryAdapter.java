package com.ticketbooking.event.infrastructure.out.persistence;

import com.ticketbooking.event.domain.model.Event;
import com.ticketbooking.event.domain.model.enums.EventStatus;
import com.ticketbooking.event.domain.port.out.EventRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EventRepositoryAdapter implements EventRepositoryPort {

    private final EventJpaRepository jpa;

    public EventRepositoryAdapter(EventJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public Event save(Event e) { return toDomain(jpa.save(toJpa(e))); }

    @Override
    public Optional<Event> findById(Long id) { return jpa.findById(id).map(this::toDomain); }

    @Override
    public List<Event> findByStatus(EventStatus status) {
        return jpa.findByStatus(status.name()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Event> findByVenueCityAndStatus(String city, EventStatus status) {
        return jpa.findByVenueCityAndStatus(city, status.name()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private EventJpaEntity toJpa(Event e) {
        return EventJpaEntity.builder()
                .id(e.getId()).name(e.getName()).description(e.getDescription())
                .startTime(e.getStartTime()).endTime(e.getEndTime())
                .basePrice(e.getBasePrice()).status(e.getStatus().name())
                .venueId(e.getVenueId()).build();
    }

    private Event toDomain(EventJpaEntity e) {
        return new Event(e.getId(), e.getName(), e.getDescription(),
                e.getStartTime(), e.getEndTime(), e.getBasePrice(),
                EventStatus.valueOf(e.getStatus()), e.getVenueId());
    }
}
