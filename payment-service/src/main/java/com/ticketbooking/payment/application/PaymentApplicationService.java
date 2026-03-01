package com.ticketbooking.payment.application;

import com.ticketbooking.payment.domain.model.Payment;
import com.ticketbooking.payment.domain.port.in.ProcessPaymentUseCase;
import com.ticketbooking.payment.domain.port.out.PaymentGatewayPort;
import com.ticketbooking.payment.infrastructure.out.messaging.AuditLogPublisher;
import org.springframework.stereotype.Service;

@Service
public class PaymentApplicationService implements ProcessPaymentUseCase {

    private final PaymentGatewayPort gateway;
    private final AuditLogPublisher auditLogPublisher;

    public PaymentApplicationService(PaymentGatewayPort gateway,
                                     AuditLogPublisher auditLogPublisher) {
        this.gateway = gateway;
        this.auditLogPublisher = auditLogPublisher;
    }

    @Override
    public String processPayment(Long bookingId, String paymentToken) {
        Payment result = gateway.charge(bookingId, paymentToken);
        if (!result.isSuccessful()) {
            auditLogPublisher.publish(
                    "PAYMENT_FAILED",
                    bookingId,
                    "FAILED",
                    "Payment gateway returned failed status"
            );
            throw new RuntimeException("Payment failed for booking: " + bookingId);
        }
        auditLogPublisher.publish(
                "PAYMENT_PROCESSED",
                bookingId,
                "SUCCESS",
                "Payment processed with transactionId=" + result.getTransactionId()
        );
        return result.getTransactionId();
    }
}
