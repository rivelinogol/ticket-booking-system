package com.ticketbooking.booking.domain.port.out;

import com.ticketbooking.booking.domain.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Output port: contrato de persistencia para Booking.
 * El dominio define QUÉ necesita. La infraestructura decide CÓMO lo implementa (JPA, MongoDB, etc.).
 */
public interface BookingRepositoryPort {
    Booking save(Booking booking);
    Optional<Booking> findById(Long id);
    List<Booking> findByUserId(Long userId);
    List<Booking> findExpiredPendingBookings(LocalDateTime now);
}
