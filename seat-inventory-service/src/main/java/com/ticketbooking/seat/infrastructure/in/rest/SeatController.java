package com.ticketbooking.seat.infrastructure.in.rest;

import com.ticketbooking.common.dto.SeatAvailabilityDTO;
import com.ticketbooking.seat.domain.port.in.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
public class SeatController {

    private final GetAvailableSeatsUseCase getAvailableSeats;
    private final LockSeatUseCase lockSeat;
    private final ConfirmSeatUseCase confirmSeat;
    private final ReleaseSeatUseCase releaseSeat;

    public SeatController(GetAvailableSeatsUseCase getAvailableSeats,
                          LockSeatUseCase lockSeat,
                          ConfirmSeatUseCase confirmSeat,
                          ReleaseSeatUseCase releaseSeat) {
        this.getAvailableSeats = getAvailableSeats;
        this.lockSeat = lockSeat;
        this.confirmSeat = confirmSeat;
        this.releaseSeat = releaseSeat;
    }

    @GetMapping("/venue/{venueId}")
    public ResponseEntity<List<SeatAvailabilityDTO>> getAvailable(@PathVariable("venueId") Long venueId) {
        return ResponseEntity.ok(getAvailableSeats.getAvailableSeats(venueId));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<Void> lock(@PathVariable("id") Long id) {
        lockSeat.lockSeat(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable("id") Long id) {
        confirmSeat.confirmSeat(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> release(@PathVariable("id") Long id) {
        releaseSeat.releaseSeat(id);
        return ResponseEntity.ok().build();
    }
}
