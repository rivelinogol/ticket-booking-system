package com.ticketbooking.payment.domain.port.in;

/**
 * Input port: procesar un pago con idempotency key.
 * bookingId actúa como idempotency key — retries no generan cobros duplicados.
 */
public interface ProcessPaymentUseCase {
    String processPayment(Long bookingId, String paymentToken);
}
