package com.ticketbooking.event.infrastructure.out.client;

import com.ticketbooking.common.dto.SeatAvailabilityDTO;
import com.ticketbooking.event.domain.port.out.SeatAvailabilityPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Component
public class SeatInventoryClientAdapter implements SeatAvailabilityPort {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SeatInventoryClientAdapter(RestTemplate restTemplate,
                                      @Value("${services.seat-inventory.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<SeatAvailabilityDTO> getAvailableSeats(Long venueId) {
        try {
            var response = restTemplate.exchange(
                    baseUrl + "/api/seats/venue/" + venueId,
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<SeatAvailabilityDTO>>() {});
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
