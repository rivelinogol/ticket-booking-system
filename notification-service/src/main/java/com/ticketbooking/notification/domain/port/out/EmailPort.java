package com.ticketbooking.notification.domain.port.out;

public interface EmailPort {
    void sendEmail(String to, String subject, String body);
}
