package com.ticketbooking.event.infrastructure.out.persistence;

import com.ticketbooking.event.domain.model.Venue;
import com.ticketbooking.event.domain.port.out.VenueRepositoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(
        name = "app.event.persistence-provider",
        havingValue = "jpa",
        matchIfMissing = true
)
public class VenueRepositoryAdapter implements VenueRepositoryPort {

    private final VenueJpaRepository jpa;

    public VenueRepositoryAdapter(VenueJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public Venue save(Venue v) { return toDomain(jpa.save(toJpa(v))); }

    @Override
    public Optional<Venue> findById(Long id) { return jpa.findById(id).map(this::toDomain); }

    private VenueJpaEntity toJpa(Venue v) {
        return VenueJpaEntity.builder()
                .id(v.getId()).name(v.getName()).address(v.getAddress())
                .city(v.getCity()).country(v.getCountry()).totalCapacity(v.getTotalCapacity()).build();
    }

    private Venue toDomain(VenueJpaEntity e) {
        return new Venue(e.getId(), e.getName(), e.getAddress(), e.getCity(), e.getCountry(), e.getTotalCapacity());
    }
}
