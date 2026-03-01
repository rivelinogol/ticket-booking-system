package com.ticketbooking.booking.infrastructure.out.messaging;

import com.ticketbooking.booking.domain.port.out.NotificationPort;
import com.ticketbooking.common.dto.BookingEventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Adaptador de salida: implementa NotificationPort.
 *
 * En producción publicaría un mensaje en Kafka topic "booking-events".
 * El notification-service lo consumiría de forma async vía @KafkaListener.
 * Esto desacopla el flujo crítico del booking del tiempo de respuesta de Twilio/SES.
 *
 */
@Component
public class NotificationMessageAdapter implements NotificationPort {

    private final KafkaTemplate<String, BookingEventDTO> kafkaTemplate;
    private final String bookingEventsTopic;

    public NotificationMessageAdapter(KafkaTemplate<String, BookingEventDTO> kafkaTemplate,
                                      @Value("${app.kafka.booking-events-topic}") String bookingEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.bookingEventsTopic = bookingEventsTopic;
    }

    @Override
    public void notifyConfirmation(Long bookingId) {
        publish(bookingId, "BOOKING_CONFIRMED");
    }

    @Override
    public void notifyCancellation(Long bookingId) {
        publish(bookingId, "BOOKING_CANCELLED");
    }

    private void publish(Long bookingId, String type) {
        BookingEventDTO event = BookingEventDTO.builder()
                .bookingId(bookingId)
                .type(type)
                .occurredAt(LocalDateTime.now())
                .build();
        kafkaTemplate.send(bookingEventsTopic, bookingId.toString(), event);
    }
}
