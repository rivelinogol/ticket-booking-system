package com.ticketbooking.seat.domain.model;

import com.ticketbooking.seat.domain.model.enums.SeatStatus;

import java.math.BigDecimal;

/**
 * Aggregate Root del bounded context de Inventario de Asientos.
 * POJO puro — cero imports de Spring o JPA.
 *
 * Regla de negocio: las transiciones de estado son métodos del agregado.
 * Invariante: solo se puede lockear un asiento AVAILABLE.
 */
public class Seat {

    private Long id;
    private Long venueId;
    private SeatPosition position;
    private String section;
    private BigDecimal price;
    private SeatStatus status;
    private Long version; // para optimistic locking

    public Seat(Long venueId, SeatPosition position, String section, BigDecimal price) {
        this.venueId = venueId;
        this.position = position;
        this.section = section;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
        this.version = 0L;
    }

    public Seat(Long id, Long venueId, SeatPosition position, String section,
                BigDecimal price, SeatStatus status, Long version) {
        this.id = id;
        this.venueId = venueId;
        this.position = position;
        this.section = section;
        this.price = price;
        this.status = status;
        this.version = version;
    }

    // ─── Comportamiento del dominio ───────────────────────────────────────────

    public void lock() {
        if (status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available: " + status);
        }
        this.status = SeatStatus.LOCKED;
    }

    public void confirm() {
        if (status != SeatStatus.LOCKED) {
            throw new IllegalStateException("Seat is not locked: " + status);
        }
        this.status = SeatStatus.BOOKED;
    }

    public void release() {
        if (status == SeatStatus.UNAVAILABLE) {
            throw new IllegalStateException("Cannot release an unavailable seat");
        }
        this.status = SeatStatus.AVAILABLE;
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVenueId() { return venueId; }
    public SeatPosition getPosition() { return position; }
    public String getSection() { return section; }
    public BigDecimal getPrice() { return price; }
    public SeatStatus getStatus() { return status; }
    public Long getVersion() { return version; }
}
