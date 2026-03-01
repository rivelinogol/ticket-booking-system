package com.ticketbooking.payment.application;

import com.ticketbooking.payment.domain.model.Payment;
import com.ticketbooking.payment.domain.port.in.ProcessPaymentUseCase;
import com.ticketbooking.payment.domain.port.out.PaymentGatewayPort;
import org.springframework.stereotype.Service;

@Service
public class PaymentApplicationService implements ProcessPaymentUseCase {

    private final PaymentGatewayPort gateway;

    public PaymentApplicationService(PaymentGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public String processPayment(Long bookingId, String paymentToken) {
        Payment result = gateway.charge(bookingId, paymentToken);
        if (!result.isSuccessful()) {
            throw new RuntimeException("Payment failed for booking: " + bookingId);
        }
        return result.getTransactionId();
    }
}
