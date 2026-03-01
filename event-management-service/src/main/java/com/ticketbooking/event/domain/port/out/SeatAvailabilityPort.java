package com.ticketbooking.event.domain.port.out;

import com.ticketbooking.common.dto.SeatAvailabilityDTO;
import java.util.List;

/**
 * Output port: obtener disponibilidad de asientos de un venue.
 * El dominio no sabe que la implementación llama a seat-inventory-service vía HTTP.
 */
public interface SeatAvailabilityPort {
    List<SeatAvailabilityDTO> getAvailableSeats(Long venueId);
}
