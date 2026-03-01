package com.ticketbooking.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentRequestDTO {
    @NotNull  private Long bookingId;
    @NotBlank private String paymentToken;
}
