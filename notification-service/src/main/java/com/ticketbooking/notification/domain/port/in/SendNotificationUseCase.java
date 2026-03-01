package com.ticketbooking.notification.domain.port.in;

import com.ticketbooking.notification.domain.model.Notification;

public interface SendNotificationUseCase {
    void send(Notification notification);
}
