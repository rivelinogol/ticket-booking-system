package com.ticketbooking.common.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatAvailabilityDTO {
    private Long seatId;
    private String rowNumber;
    private int seatNumber;
    private String section;
    private BigDecimal price;
    private String status; // AVAILABLE, LOCKED, BOOKED
}
