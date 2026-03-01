package com.ticketbooking.booking.domain.port.out;

public interface AuditLogPort {
    void log(String action, Long bookingId, Long userId, String status, String details);
}
