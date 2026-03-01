package com.ticketbooking.event.infrastructure.out.persistence.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "venues")
public class VenueMongoDocument {
    @Id
    private Long id;
    private String name;
    private String address;
    private String city;
    private String country;
    private int totalCapacity;
}
