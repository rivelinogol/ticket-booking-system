package com.ticketbooking.booking.domain.model;

import com.ticketbooking.booking.domain.model.enums.BookingStatus;

import java.time.LocalDateTime;

/**
 * Aggregate Root del bounded context de Reservas.
 *
 * POJO puro — cero imports de Spring o JPA.
 * Contiene la lógica de negocio: las transiciones de estado son métodos
 * del agregado, no del application service (modelo de dominio RICO, no anémico).
 *
 * Invariante: solo se puede confirmar/cancelar/expirar un booking PENDING.
 */
public class Booking {

    private Long id;
    private Long userId;
    private Long eventId;
    private Long seatId;
    private Money totalPrice;
    private BookingStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime bookingTime;
    private LocalDateTime updatedAt;
    private String paymentTransactionId;

    // Constructor de creación (factory method en application service)
    public Booking(Long userId, Long eventId, Long seatId, Money totalPrice, LocalDateTime expiresAt) {
        this.userId = userId;
        this.eventId = eventId;
        this.seatId = seatId;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.PENDING;
        this.expiresAt = expiresAt;
        this.bookingTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor de reconstitución (usado por el repositorio al cargar de DB)
    public Booking(Long id, Long userId, Long eventId, Long seatId, Money totalPrice,
                   BookingStatus status, LocalDateTime expiresAt, LocalDateTime bookingTime,
                   LocalDateTime updatedAt, String paymentTransactionId) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.seatId = seatId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.expiresAt = expiresAt;
        this.bookingTime = bookingTime;
        this.updatedAt = updatedAt;
        this.paymentTransactionId = paymentTransactionId;
    }

    // ─── Comportamiento del dominio ───────────────────────────────────────────

    /**
     * Confirma la reserva tras un pago exitoso.
     * Invariante: solo PENDING puede confirmarse.
     */
    public void confirm(String transactionId) {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot confirm booking in status: " + status);
        }
        this.status = BookingStatus.CONFIRMED;
        this.paymentTransactionId = transactionId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Cancela la reserva.
     * Invariante: no se puede cancelar lo que ya está EXPIRED.
     */
    public void cancel() {
        if (status == BookingStatus.EXPIRED) {
            throw new IllegalStateException("Cannot cancel an expired booking");
        }
        this.status = BookingStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Expira la reserva cuando el TTL venció sin pago.
     * Invariante: solo PENDING puede expirar.
     */
    public void expire() {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot expire booking in status: " + status);
        }
        this.status = BookingStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return status == BookingStatus.PENDING
            && LocalDateTime.now().isAfter(expiresAt);
    }

    // ─── Getters (sin setters — el estado cambia solo por métodos de dominio) ─

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; } // solo para el repositorio
    public Long getUserId() { return userId; }
    public Long getEventId() { return eventId; }
    public Long getSeatId() { return seatId; }
    public Money getTotalPrice() { return totalPrice; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getBookingTime() { return bookingTime; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getPaymentTransactionId() { return paymentTransactionId; }
}
