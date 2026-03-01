package com.ticketbooking.event.infrastructure.in.rest;

import com.ticketbooking.common.dto.EventDTO;
import com.ticketbooking.event.domain.port.in.CreateEventUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CreateEventUseCase createEvent;

    public AdminController(CreateEventUseCase createEvent) {
        this.createEvent = createEvent;
    }

    @PostMapping("/events")
    public ResponseEntity<EventDTO> create(@RequestBody EventDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createEvent.createEvent(request));
    }
}
