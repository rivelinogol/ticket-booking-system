package com.ticketbooking.booking.domain.port.in;

import com.ticketbooking.common.dto.BookingRequestDTO;
import com.ticketbooking.common.dto.BookingResponseDTO;

/**
 * Input port: caso de uso para iniciar una reserva.
 * Paso 1 del flujo: lockea el asiento y crea el booking en estado PENDING.
 */
public interface InitiateBookingUseCase {
    BookingResponseDTO initiateBooking(BookingRequestDTO request);
}
