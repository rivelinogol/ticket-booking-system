package com.ticketbooking.booking.domain.port.in;

import com.ticketbooking.common.dto.BookingResponseDTO;
import com.ticketbooking.common.dto.PaymentRequestDTO;

/**
 * Input port: caso de uso para confirmar el pago de una reserva.
 * Paso 2 del flujo: procesa el pago y confirma el asiento.
 */
public interface ConfirmPaymentUseCase {
    BookingResponseDTO confirmPayment(PaymentRequestDTO request);
}
