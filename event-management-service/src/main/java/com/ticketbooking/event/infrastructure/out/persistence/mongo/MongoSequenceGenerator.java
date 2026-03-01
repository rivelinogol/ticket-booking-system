package com.ticketbooking.event.infrastructure.out.persistence.mongo;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class MongoSequenceGenerator {

    private final MongoOperations mongoOperations;

    public MongoSequenceGenerator(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public long next(String sequenceName) {
        Query query = new Query(Criteria.where("_id").is(sequenceName));
        Update update = new Update().inc("seq", 1);

        DatabaseSequence counter = mongoOperations.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                DatabaseSequence.class
        );

        return counter != null ? counter.getSeq() : 1L;
    }
}
