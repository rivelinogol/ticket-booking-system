package com.ticketbooking.notification.domain.model;

/**
 * Aggregate Root del bounded context de Notificaciones. POJO puro.
 */
public class Notification {

    private Long bookingId;
    private String recipientEmail;
    private String recipientPhone;
    private NotificationType type;

    public Notification(Long bookingId, String recipientEmail, String recipientPhone, NotificationType type) {
        this.bookingId = bookingId;
        this.recipientEmail = recipientEmail;
        this.recipientPhone = recipientPhone;
        this.type = type;
    }

    public Long getBookingId() { return bookingId; }
    public String getRecipientEmail() { return recipientEmail; }
    public String getRecipientPhone() { return recipientPhone; }
    public NotificationType getType() { return type; }

    public enum NotificationType { BOOKING_CONFIRMED, BOOKING_CANCELLED, PAYMENT_FAILED }
}
