package com.ticketbooking.booking.domain.port.in;

import com.ticketbooking.common.dto.BookingResponseDTO;

import java.util.List;

/**
 * Input port: obtener el historial de reservas de un usuario.
 */
public interface GetUserBookingsUseCase {
    List<BookingResponseDTO> getUserBookings(Long userId);
}
