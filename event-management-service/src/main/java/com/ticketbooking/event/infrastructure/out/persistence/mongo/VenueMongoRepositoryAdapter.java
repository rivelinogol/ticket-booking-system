package com.ticketbooking.event.infrastructure.out.persistence.mongo;

import com.ticketbooking.event.domain.model.Venue;
import com.ticketbooking.event.domain.port.out.VenueRepositoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.event.persistence-provider", havingValue = "mongo")
public class VenueMongoRepositoryAdapter implements VenueRepositoryPort {

    private final MongoTemplate mongoTemplate;
    private final MongoSequenceGenerator sequenceGenerator;

    public VenueMongoRepositoryAdapter(MongoTemplate mongoTemplate,
                                       MongoSequenceGenerator sequenceGenerator) {
        this.mongoTemplate = mongoTemplate;
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public Venue save(Venue venue) {
        VenueMongoDocument doc = toDocument(venue);
        if (doc.getId() == null) {
            doc.setId(sequenceGenerator.next("venues"));
        }
        return toDomain(mongoTemplate.save(doc));
    }

    @Override
    public Optional<Venue> findById(Long id) {
        VenueMongoDocument doc = mongoTemplate.findById(id, VenueMongoDocument.class);
        return Optional.ofNullable(doc).map(this::toDomain);
    }

    private VenueMongoDocument toDocument(Venue venue) {
        return VenueMongoDocument.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .country(venue.getCountry())
                .totalCapacity(venue.getTotalCapacity())
                .build();
    }

    private Venue toDomain(VenueMongoDocument doc) {
        return new Venue(
                doc.getId(),
                doc.getName(),
                doc.getAddress(),
                doc.getCity(),
                doc.getCountry(),
                doc.getTotalCapacity()
        );
    }
}
