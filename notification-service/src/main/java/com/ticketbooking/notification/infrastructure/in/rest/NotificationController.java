package com.ticketbooking.notification.infrastructure.in.rest;

import com.ticketbooking.notification.domain.model.Notification;
import com.ticketbooking.notification.domain.port.in.SendNotificationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint manual para pruebas locales.
 * Además del endpoint HTTP, el servicio consume eventos async vía @KafkaListener.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SendNotificationUseCase sendNotification;

    public NotificationController(SendNotificationUseCase sendNotification) {
        this.sendNotification = sendNotification;
    }

    @PostMapping
    public ResponseEntity<Void> notify(@RequestBody Map<String, String> body) {
        Notification notification = new Notification(
                Long.parseLong(body.get("bookingId")),
                body.get("email"),
                body.get("phone"),
                Notification.NotificationType.valueOf(body.get("type"))
        );
        sendNotification.send(notification);
        return ResponseEntity.ok().build();
    }
}
