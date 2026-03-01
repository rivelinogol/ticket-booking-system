package com.ticketbooking.notification.infrastructure.out.sms;

import com.ticketbooking.notification.domain.port.out.SmsPort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida: stub de Twilio.
 * En producción usaría el SDK de Twilio para enviar SMS.
 */
@Component
public class TwilioSmsAdapter implements SmsPort {

    @Override
    public void sendSms(String to, String message) {
        // TODO: Message.creator(new PhoneNumber(to), ...).create()
        System.out.println("[TWILIO STUB] To: " + to + " | Message: " + message);
    }
}
