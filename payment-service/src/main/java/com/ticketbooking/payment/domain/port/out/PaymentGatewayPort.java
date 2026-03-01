package com.ticketbooking.payment.domain.port.out;

import com.ticketbooking.payment.domain.model.Payment;

/**
 * Output port: contrato para el proveedor de pagos externo.
 * El dominio no sabe si la implementación usa Stripe, Razorpay u otro.
 */
public interface PaymentGatewayPort {
    Payment charge(Long bookingId, String paymentToken);
}
