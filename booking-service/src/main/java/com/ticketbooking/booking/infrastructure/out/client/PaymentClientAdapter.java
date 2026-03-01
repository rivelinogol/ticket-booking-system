package com.ticketbooking.booking.infrastructure.out.client;

import com.ticketbooking.booking.domain.port.out.PaymentPort;
import com.ticketbooking.common.dto.PaymentRequestDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Adaptador de salida: implementa PaymentPort llamando a payment-service vía HTTP.
 * Decisión de diseño: Idempotency Keys.
 * bookingId actúa como idempotency key — si la red falla y se reintenta,
 * el payment-service retorna el mismo transactionId sin cobrar dos veces.
 */
@Component
public class PaymentClientAdapter implements PaymentPort {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PaymentClientAdapter(RestTemplate restTemplate,
                                @Value("${services.payment.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public String processPayment(Long bookingId, String paymentToken) {
        PaymentRequestDTO request = new PaymentRequestDTO(bookingId, paymentToken);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", bookingId.toString());
        HttpEntity<PaymentRequestDTO> httpEntity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(baseUrl + "/api/payments/process", httpEntity, String.class);
    }
}
