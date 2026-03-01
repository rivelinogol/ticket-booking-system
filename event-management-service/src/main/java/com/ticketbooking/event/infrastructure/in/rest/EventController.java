package com.ticketbooking.event.infrastructure.in.rest;

import com.ticketbooking.common.dto.EventDTO;
import com.ticketbooking.common.dto.SeatAvailabilityDTO;
import com.ticketbooking.event.domain.port.in.GetEventDetailsUseCase;
import com.ticketbooking.event.domain.port.in.GetEventsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final GetEventsUseCase getEvents;
    private final GetEventDetailsUseCase getEventDetails;

    public EventController(GetEventsUseCase getEvents, GetEventDetailsUseCase getEventDetails) {
        this.getEvents = getEvents;
        this.getEventDetails = getEventDetails;
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> list(@RequestParam(name = "city", required = false) String city) {
        List<EventDTO> events = city != null
                ? getEvents.getEventsByCity(city)
                : getEvents.getPublishedEvents();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> detail(@PathVariable("id") Long id) {
        return ResponseEntity.ok(getEventDetails.getEventById(id));
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<List<SeatAvailabilityDTO>> seats(@PathVariable("id") Long id) {
        return ResponseEntity.ok(getEventDetails.getSeatAvailability(id));
    }
}
