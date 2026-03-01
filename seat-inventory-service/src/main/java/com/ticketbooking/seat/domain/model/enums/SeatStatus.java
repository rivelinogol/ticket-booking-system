package com.ticketbooking.seat.domain.model.enums;

public enum SeatStatus {
    AVAILABLE,
    LOCKED,      // TTL 5 min en Redis
    BOOKED,
    UNAVAILABLE
}
