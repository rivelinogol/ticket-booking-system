package com.ticketbooking.payment.infrastructure.in.rest;

import com.ticketbooking.common.dto.PaymentRequestDTO;
import com.ticketbooking.payment.domain.port.in.ProcessPaymentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPayment;
    private final WebhookSignatureVerifier signatureVerifier;

    public PaymentController(ProcessPaymentUseCase processPayment,
                             WebhookSignatureVerifier signatureVerifier) {
        this.processPayment = processPayment;
        this.signatureVerifier = signatureVerifier;
    }

    @PostMapping("/process")
    public ResponseEntity<String> process(
            @RequestBody PaymentRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.equals(request.getBookingId().toString())) {
            return ResponseEntity.badRequest().body("Idempotency-Key must match bookingId");
        }
        String txId = processPayment.processPayment(request.getBookingId(), request.getPaymentToken());
        return ResponseEntity.ok(txId);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sig) {
        if (!signatureVerifier.isValid(payload, sig)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }
}
