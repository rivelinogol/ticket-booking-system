package com.ticketbooking.notification.infrastructure.in.messaging;

import com.ticketbooking.common.dto.BookingEventDTO;
import com.ticketbooking.notification.domain.model.Notification;
import com.ticketbooking.notification.domain.port.in.SendNotificationUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingEventsKafkaListener {

    private final SendNotificationUseCase sendNotification;

    public BookingEventsKafkaListener(SendNotificationUseCase sendNotification) {
        this.sendNotification = sendNotification;
    }

    @KafkaListener(
            topics = "${app.kafka.booking-events-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(BookingEventDTO event) {
        Notification.NotificationType type = mapType(event.getType());
        Notification notification = new Notification(
                event.getBookingId(),
                null,
                null,
                type
        );
        sendNotification.send(notification);
    }

    private Notification.NotificationType mapType(String type) {
        if ("BOOKING_CANCELLED".equalsIgnoreCase(type)) {
            return Notification.NotificationType.BOOKING_CANCELLED;
        }
        if ("PAYMENT_FAILED".equalsIgnoreCase(type)) {
            return Notification.NotificationType.PAYMENT_FAILED;
        }
        return Notification.NotificationType.BOOKING_CONFIRMED;
    }
}
