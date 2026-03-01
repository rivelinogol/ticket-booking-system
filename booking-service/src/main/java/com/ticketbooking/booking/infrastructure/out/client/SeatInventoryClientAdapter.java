package com.ticketbooking.booking.infrastructure.out.client;

import com.ticketbooking.booking.domain.port.out.SeatLockingPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Adaptador de salida: implementa SeatLockingPort llamando a seat-inventory-service vía HTTP.
 * El dominio solo conoce SeatLockingPort — no sabe que hay HTTP de por medio.
 *
 * En producción: agregar Circuit Breaker (Resilience4j) para evitar cascading failures.
 */
@Component
public class SeatInventoryClientAdapter implements SeatLockingPort {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SeatInventoryClientAdapter(RestTemplate restTemplate,
                                      @Value("${services.seat-inventory.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public void lock(Long seatId) {
        restTemplate.postForEntity(baseUrl + "/api/seats/" + seatId + "/lock", null, Void.class);
    }

    @Override
    public void confirm(Long seatId) {
        restTemplate.postForEntity(baseUrl + "/api/seats/" + seatId + "/confirm", null, Void.class);
    }

    @Override
    public void release(Long seatId) {
        restTemplate.postForEntity(baseUrl + "/api/seats/" + seatId + "/release", null, Void.class);
    }
}
