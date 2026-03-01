package com.ticketbooking.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingRequestDTO {
    @NotNull private Long userId;
    @NotNull private Long eventId;
    @NotNull private Long seatId;
}
