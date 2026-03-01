package com.ticketbooking.notification.application;

import com.ticketbooking.notification.domain.model.Notification;
import com.ticketbooking.notification.domain.port.in.SendNotificationUseCase;
import com.ticketbooking.notification.domain.port.out.EmailPort;
import com.ticketbooking.notification.domain.port.out.SmsPort;
import org.springframework.stereotype.Service;

@Service
public class NotificationApplicationService implements SendNotificationUseCase {

    private final EmailPort email;
    private final SmsPort sms;

    public NotificationApplicationService(EmailPort email, SmsPort sms) {
        this.email = email;
        this.sms = sms;
    }

    @Override
    public void send(Notification notification) {
        String subject = switch (notification.getType()) {
            case BOOKING_CONFIRMED -> "Booking Confirmed!";
            case BOOKING_CANCELLED -> "Booking Cancelled";
            case PAYMENT_FAILED -> "Payment Failed";
        };
        if (notification.getRecipientEmail() != null && !notification.getRecipientEmail().isBlank()) {
            email.sendEmail(notification.getRecipientEmail(), subject,
                    "Your booking #" + notification.getBookingId() + " — " + notification.getType());
        }

        if (notification.getRecipientPhone() != null && !notification.getRecipientPhone().isBlank()) {
            sms.sendSms(notification.getRecipientPhone(), subject + " — Booking #" + notification.getBookingId());
        }
    }
}
