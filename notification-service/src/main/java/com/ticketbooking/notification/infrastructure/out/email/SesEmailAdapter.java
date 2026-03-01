package com.ticketbooking.notification.infrastructure.out.email;

import com.ticketbooking.notification.domain.port.out.EmailPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: stub de AWS SES.
 * En producción usaría el SDK de AWS SES para enviar emails.
 */
@Component
public class SesEmailAdapter implements EmailPort {

    @Override
    public void sendEmail(String to, String subject, String body) {
        // TODO: SesClient.sendEmail(...)
        System.out.println("[SES STUB] To: " + to + " | Subject: " + subject);
    }
}
