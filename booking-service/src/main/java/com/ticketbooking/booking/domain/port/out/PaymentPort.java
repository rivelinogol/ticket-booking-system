package com.ticketbooking.booking.domain.port.out;

/**
 * Output port: contrato para procesar pagos.
 * El dominio no sabe si la implementación usa Stripe, Razorpay u otro proveedor.
 * Retorna el ID de transacción del proveedor (para audit logs).
 */
public interface PaymentPort {
    String processPayment(Long bookingId, String paymentToken);
}
