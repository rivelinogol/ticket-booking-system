package com.ticketbooking.audit.infrastructure.in.messaging;

import com.ticketbooking.audit.infrastructure.out.persistence.AuditLogJpaEntity;
import com.ticketbooking.audit.infrastructure.out.persistence.AuditLogJpaRepository;
import com.ticketbooking.common.dto.AuditLogEventDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditLogKafkaConsumer {

    private final AuditLogJpaRepository repository;

    public AuditLogKafkaConsumer(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "${app.kafka.audit-log-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(AuditLogEventDTO event) {
        AuditLogJpaEntity entity = AuditLogJpaEntity.builder()
                .service(event.getService())
                .action(event.getAction())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .userId(event.getUserId())
                .status(event.getStatus())
                .details(event.getDetails())
                .timestamp(event.getTimestamp())
                .build();
        repository.save(entity);
    }
}
