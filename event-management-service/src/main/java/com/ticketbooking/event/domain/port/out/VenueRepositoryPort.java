package com.ticketbooking.event.domain.port.out;

import com.ticketbooking.event.domain.model.Venue;
import java.util.Optional;

public interface VenueRepositoryPort {
    Venue save(Venue venue);
    Optional<Venue> findById(Long id);
}
