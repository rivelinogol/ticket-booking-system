package com.ticketbooking.event.application;

import com.ticketbooking.common.dto.EventDTO;
import com.ticketbooking.common.dto.SeatAvailabilityDTO;
import com.ticketbooking.common.exception.EventNotFoundException;
import com.ticketbooking.event.domain.model.Event;
import com.ticketbooking.event.domain.model.Venue;
import com.ticketbooking.event.domain.model.enums.EventStatus;
import com.ticketbooking.event.domain.port.in.*;
import com.ticketbooking.event.domain.port.out.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventApplicationService
        implements GetEventsUseCase, GetEventDetailsUseCase, CreateEventUseCase {

    private final EventRepositoryPort eventRepository;
    private final VenueRepositoryPort venueRepository;
    private final SeatAvailabilityPort seatAvailability;

    public EventApplicationService(EventRepositoryPort eventRepository,
                                   VenueRepositoryPort venueRepository,
                                   SeatAvailabilityPort seatAvailability) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.seatAvailability = seatAvailability;
    }

    @Override
    public List<EventDTO> getPublishedEvents() {
        return eventRepository.findByStatus(EventStatus.PUBLISHED)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<EventDTO> getEventsByCity(String city) {
        return eventRepository.findByVenueCityAndStatus(city, EventStatus.PUBLISHED)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public EventDTO getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        return toDTO(event);
    }

    @Override
    public List<SeatAvailabilityDTO> getSeatAvailability(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        return seatAvailability.getAvailableSeats(event.getVenueId());
    }

    @Override
    public EventDTO createEvent(EventDTO request) {
        Event event = new Event(
                request.getName(), request.getDescription(),
                request.getStartTime(), request.getBasePrice(),
                null // TODO: venueId desde request
        );
        return toDTO(eventRepository.save(event));
    }

    private EventDTO toDTO(Event event) {
        Venue venue = event.getVenueId() != null
                ? venueRepository.findById(event.getVenueId()).orElse(null)
                : null;

        return EventDTO.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .basePrice(event.getBasePrice())
                .status(event.getStatus().name())
                .venueName(venue != null ? venue.getName() : null)
                .venueCity(venue != null ? venue.getCity() : null)
                .venueCountry(venue != null ? venue.getCountry() : null)
                .build();
    }
}
