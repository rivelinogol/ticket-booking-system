package com.ticketbooking.booking.domain.port.in;

/**
 * Input port: caso de uso para liberar reservas expiradas.
 * Un job periódico llama a esto para liberar asientos cuyo TTL venció sin pago.
 * (Constraint: "Handling payment failures - release locked seats quickly")
 */
public interface ReleaseExpiredBookingsUseCase {
    void releaseExpiredBookings();
}
