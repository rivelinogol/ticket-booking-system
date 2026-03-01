package com.ticketbooking.booking.infrastructure.out.messaging;

import com.ticketbooking.booking.domain.port.out.AuditLogPort;
import com.ticketbooking.common.dto.AuditLogEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditLogMessageAdapter implements AuditLogPort {

    private static final Logger log = LoggerFactory.getLogger(AuditLogMessageAdapter.class);
    private static final String SERVICE_NAME = "booking-service";

    private final KafkaTemplate<String, AuditLogEventDTO> kafkaTemplate;
    private final String auditLogTopic;

    public AuditLogMessageAdapter(KafkaTemplate<String, AuditLogEventDTO> kafkaTemplate,
                                  @Value("${app.kafka.audit-log-topic}") String auditLogTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditLogTopic = auditLogTopic;
    }

    @Override
    public void log(String action, Long bookingId, Long userId, String status, String details) {
        AuditLogEventDTO event = AuditLogEventDTO.builder()
                .service(SERVICE_NAME)
                .action(action)
                .resourceType("BOOKING")
                .resourceId(bookingId)
                .userId(userId)
                .status(status)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        try {
            kafkaTemplate.send(auditLogTopic, String.valueOf(bookingId), event);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish audit event action={} bookingId={}", action, bookingId, ex);
        }
    }
}
