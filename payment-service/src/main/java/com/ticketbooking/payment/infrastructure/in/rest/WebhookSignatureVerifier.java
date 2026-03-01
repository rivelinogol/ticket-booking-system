package com.ticketbooking.payment.infrastructure.in.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class WebhookSignatureVerifier {

    private final String webhookSecret;

    public WebhookSignatureVerifier(@Value("${payment.webhook.secret:dev-webhook-secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public boolean isValid(String payload, String signatureHeader) {
        if (payload == null || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        String provided = extractV1(signatureHeader);
        if (provided == null || provided.isBlank()) {
            return false;
        }

        String expected = hmacSha256Hex(payload, webhookSecret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String extractV1(String signatureHeader) {
        if (!signatureHeader.contains(",")) {
            return signatureHeader.trim();
        }

        String[] parts = signatureHeader.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("v1=") && trimmed.length() > 3) {
                return trimmed.substring(3);
            }
        }
        return null;
    }

    private String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify webhook signature", e);
        }
    }
}
