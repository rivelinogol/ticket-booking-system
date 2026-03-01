package com.ticketbooking.booking.infrastructure.out.persistence;

import com.ticketbooking.booking.domain.model.Booking;
import com.ticketbooking.booking.domain.model.Money;
import com.ticketbooking.booking.domain.model.enums.BookingStatus;
import com.ticketbooking.booking.domain.port.out.BookingRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adaptador de salida: implementa BookingRepositoryPort usando JPA.
 * Contiene el mapper entre el modelo de dominio y la entidad JPA.
 */
@Component
public class BookingRepositoryAdapter implements BookingRepositoryPort {

    private final BookingJpaRepository jpaRepository;

    public BookingRepositoryAdapter(BookingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Booking save(Booking booking) {
        BookingJpaEntity entity = toJpa(booking);
        BookingJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Booking> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findExpiredPendingBookings(LocalDateTime now) {
        return jpaRepository.findExpiredPending(now).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ─── Mappers dominio ↔ JPA ───────────────────────────────────────────────

    private BookingJpaEntity toJpa(Booking b) {
        return BookingJpaEntity.builder()
                .id(b.getId())
                .userId(b.getUserId())
                .eventId(b.getEventId())
                .seatId(b.getSeatId())
                .totalPrice(b.getTotalPrice().getAmount())
                .status(b.getStatus().name())
                .expiresAt(b.getExpiresAt())
                .bookingTime(b.getBookingTime() != null ? b.getBookingTime() : LocalDateTime.now())
                .updatedAt(b.getUpdatedAt() != null ? b.getUpdatedAt() : LocalDateTime.now())
                .paymentTransactionId(b.getPaymentTransactionId())
                .build();
    }

    private Booking toDomain(BookingJpaEntity e) {
        return new Booking(
                e.getId(),
                e.getUserId(),
                e.getEventId(),
                e.getSeatId(),
                Money.of(e.getTotalPrice()),
                BookingStatus.valueOf(e.getStatus()),
                e.getExpiresAt(),
                e.getBookingTime(),
                e.getUpdatedAt(),
                e.getPaymentTransactionId()
        );
    }
}
