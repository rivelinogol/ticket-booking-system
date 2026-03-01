package com.ticketbooking.booking.domain.port.out;

/**
 * Output port: contrato para enviar notificaciones.
 * El dominio no sabe si usa Kafka, RabbitMQ o llamada directa a notification-service.
 */
public interface NotificationPort {
    void notifyConfirmation(Long bookingId);
    void notifyCancellation(Long bookingId);
}
