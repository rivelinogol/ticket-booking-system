package com.ticketbooking.booking.domain.port.in;

/**
 * Input port: caso de uso para cancelar una reserva.
 * Libera el asiento y marca el booking como CANCELLED.
 */
public interface CancelBookingUseCase {
    void cancelBooking(Long bookingId);
}
