package com.ticketbooking.payment.infrastructure.out.gateway;

import com.ticketbooking.payment.domain.model.Payment;
import com.ticketbooking.payment.domain.port.out.PaymentGatewayPort;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Adaptador de salida: simula Stripe con idempotencia por bookingId.
 * Si se invoca más de una vez con el mismo bookingId devuelve el mismo transactionId.
 *
 * Stripe stripe = Stripe.apiKey(secretKey);
 * PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
 *     .setAmount(amount).setCurrency("usd").setPaymentMethod(paymentToken)
 *     .putExtraParam("idempotency_key", bookingId.toString())
 *     .build();
 */
@Component
public class StripePaymentAdapter implements PaymentGatewayPort {

    private final ConcurrentMap<Long, Payment> idempotentPayments = new ConcurrentHashMap<>();

    @Override
    public Payment charge(Long bookingId, String paymentToken) {
        return idempotentPayments.computeIfAbsent(bookingId, ignored -> {
            if (paymentToken == null || paymentToken.isBlank() || paymentToken.toLowerCase().contains("fail")) {
                return new Payment(bookingId, "stripe_failed_" + UUID.randomUUID(), "FAILED");
            }
            String txId = "stripe_txn_" + UUID.randomUUID();
            return new Payment(bookingId, txId, "SUCCESS");
        });
    }
}
