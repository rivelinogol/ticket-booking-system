package com.ticketbooking.common.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingResponseDTO {
    private Long bookingId;
    private String status;
    private String eventName;
    private String seatInfo;
    private BigDecimal totalPrice;
    private LocalDateTime expiresAt;
    private LocalDateTime bookingTime;
}
