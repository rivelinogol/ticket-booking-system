package com.ticketbooking.event.domain.model;

import com.ticketbooking.event.domain.model.enums.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Aggregate Root del bounded context de Eventos.
 * POJO puro. Referencia a Venue por ID (venueId), no por objeto — patrón DDD.
 * Esto mantiene los aggregates independientes y evita cargas innecesarias.
 */
public class Event {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    private EventStatus status;
    private Long venueId; // referencia por ID, no por objeto

    public Event(String name, String description, LocalDateTime startTime,
                 BigDecimal basePrice, Long venueId) {
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.basePrice = basePrice;
        this.venueId = venueId;
        this.status = EventStatus.DRAFT;
    }

    public Event(Long id, String name, String description, LocalDateTime startTime,
                 LocalDateTime endTime, BigDecimal basePrice, EventStatus status, Long venueId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        this.status = status;
        this.venueId = venueId;
    }

    public void publish() {
        if (status != EventStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT events can be published");
        }
        this.status = EventStatus.PUBLISHED;
    }

    public void cancel() {
        if (status == EventStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed event");
        }
        this.status = EventStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BigDecimal getBasePrice() { return basePrice; }
    public EventStatus getStatus() { return status; }
    public Long getVenueId() { return venueId; }
}
