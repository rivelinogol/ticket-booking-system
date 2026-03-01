package com.ticketbooking.event.infrastructure.out.persistence.mongo;

import com.ticketbooking.event.domain.model.Event;
import com.ticketbooking.event.domain.model.enums.EventStatus;
import com.ticketbooking.event.domain.port.out.EventRepositoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "app.event.persistence-provider", havingValue = "mongo")
public class EventMongoRepositoryAdapter implements EventRepositoryPort {

    private final MongoTemplate mongoTemplate;
    private final MongoSequenceGenerator sequenceGenerator;

    public EventMongoRepositoryAdapter(MongoTemplate mongoTemplate,
                                       MongoSequenceGenerator sequenceGenerator) {
        this.mongoTemplate = mongoTemplate;
        this.sequenceGenerator = sequenceGenerator;
    }

    @Override
    public Event save(Event event) {
        EventMongoDocument doc = toDocument(event);
        if (doc.getId() == null) {
            doc.setId(sequenceGenerator.next("events"));
        }
        return toDomain(mongoTemplate.save(doc));
    }

    @Override
    public Optional<Event> findById(Long id) {
        EventMongoDocument doc = mongoTemplate.findById(id, EventMongoDocument.class);
        return Optional.ofNullable(doc).map(this::toDomain);
    }

    @Override
    public List<Event> findByStatus(EventStatus status) {
        Query query = Query.query(Criteria.where("status").is(status.name()));
        return mongoTemplate.find(query, EventMongoDocument.class)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Event> findByVenueCityAndStatus(String city, EventStatus status) {
        Query venuesByCity = Query.query(Criteria.where("city").is(city));
        Set<Long> venueIds = mongoTemplate.find(venuesByCity, VenueMongoDocument.class)
                .stream().map(VenueMongoDocument::getId).collect(Collectors.toSet());

        if (venueIds.isEmpty()) {
            return List.of();
        }

        Query query = Query.query(new Criteria().andOperator(
                Criteria.where("status").is(status.name()),
                Criteria.where("venueId").in(venueIds)
        ));

        return mongoTemplate.find(query, EventMongoDocument.class)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    private EventMongoDocument toDocument(Event event) {
        return EventMongoDocument.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .basePrice(event.getBasePrice())
                .status(event.getStatus().name())
                .venueId(event.getVenueId())
                .build();
    }

    private Event toDomain(EventMongoDocument doc) {
        return new Event(
                doc.getId(),
                doc.getName(),
                doc.getDescription(),
                doc.getStartTime(),
                doc.getEndTime(),
                doc.getBasePrice(),
                EventStatus.valueOf(doc.getStatus()),
                doc.getVenueId()
        );
    }
}
