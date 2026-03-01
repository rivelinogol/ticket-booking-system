package com.ticketbooking.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEventDTO {
    private String service;
    private String action;
    private String resourceType;
    private Long resourceId;
    private Long userId;
    private String status;
    private String details;
    private LocalDateTime timestamp;
}
