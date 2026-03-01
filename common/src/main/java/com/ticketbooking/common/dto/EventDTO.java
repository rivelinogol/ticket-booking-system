package com.ticketbooking.common.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private BigDecimal basePrice;
    private String status;
    private String venueName;
    private String venueCity;
    private String venueCountry;
    private int availableSeats;
    private int totalSeats;
}
