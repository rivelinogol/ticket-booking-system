package com.ticketbooking.event.infrastructure.out.persistence.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "db_sequences")
public class DatabaseSequence {
    @Id
    private String id;
    private long seq;
}
