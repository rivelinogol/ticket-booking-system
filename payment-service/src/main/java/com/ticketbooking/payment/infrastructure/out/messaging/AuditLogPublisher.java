package com.ticketbooking.payment.infrastructure.out.messaging;

import com.ticketbooking.common.dto.AuditLogEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuditLogPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPublisher.class);
    private static final String SERVICE_NAME = "payment-service";

    private final KafkaTemplate<String, AuditLogEventDTO> kafkaTemplate;
    private final String auditTopic;

    public AuditLogPublisher(KafkaTemplate<String, AuditLogEventDTO> kafkaTemplate,
                             @Value("${app.kafka.audit-log-topic}") String auditTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.auditTopic = auditTopic;
    }

    public void publish(String action, Long bookingId, String status, String details) {
        AuditLogEventDTO event = AuditLogEventDTO.builder()
                .service(SERVICE_NAME)
                .action(action)
                .resourceType("PAYMENT")
                .resourceId(bookingId)
                .userId(null)
                .status(status)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        try {
            kafkaTemplate.send(auditTopic, String.valueOf(bookingId), event);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish payment audit action={} bookingId={}", action, bookingId, ex);
        }
    }
}
