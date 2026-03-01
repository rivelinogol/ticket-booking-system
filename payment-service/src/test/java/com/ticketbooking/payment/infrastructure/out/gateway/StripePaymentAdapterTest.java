package com.ticketbooking.payment.infrastructure.out.gateway;

import com.ticketbooking.payment.domain.model.Payment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StripePaymentAdapterTest {

    @Test
    void shouldBeIdempotentForSameBookingId() {
        StripePaymentAdapter adapter = new StripePaymentAdapter();

        Payment first = adapter.charge(42L, "tok_visa");
        Payment second = adapter.charge(42L, "tok_visa");

        assertTrue(first.isSuccessful());
        assertTrue(second.isSuccessful());
        assertEquals(first.getTransactionId(), second.getTransactionId());
    }

    @Test
    void shouldReturnFailedPaymentForFailingToken() {
        StripePaymentAdapter adapter = new StripePaymentAdapter();

        Payment payment = adapter.charge(99L, "fail_card");

        assertFalse(payment.isSuccessful());
    }
}
