package com.ticketbooking.payment.infrastructure.in.rest;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureVerifierTest {

    @Test
    void shouldValidateStripeStyleSignatureHeader() throws Exception {
        String payload = "{\"id\":\"evt_1\"}";
        String secret = "unit-test-secret";
        String signature = hmacSha256Hex(payload, secret);
        WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(secret);

        assertTrue(verifier.isValid(payload, "t=1700000000,v1=" + signature));
    }

    @Test
    void shouldRejectInvalidSignature() {
        WebhookSignatureVerifier verifier = new WebhookSignatureVerifier("unit-test-secret");
        assertFalse(verifier.isValid("{\"id\":\"evt_1\"}", "v1=invalid"));
    }

    private static String hmacSha256Hex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
