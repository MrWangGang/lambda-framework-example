package com.lambda.example.servicea.repository;

import com.lambda.example.servicea.po.CatalogEventPO;
import com.mongodb.client.result.UpdateResult;
import jakarta.annotation.Resource;
import org.bson.types.ObjectId;
import org.lambda.framework.repository.operation.MongoAuditUpdates;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public class CatalogEventRepository {
    @Resource
    private ReactiveMongoTemplate mongo;

    public Mono<CatalogEventPO> findByEventId(String eventId) {
        return mongo.findOne(eventQuery(eventId), CatalogEventPO.class);
    }

    public Mono<UpdateResult> saveIfAbsent(String eventId, ObjectId productId, Instant createdAt) {
        var update = new Update().setOnInsert("eventId", eventId).setOnInsert("productId", productId);
        return mongo.upsert(eventQuery(eventId), MongoAuditUpdates.insertOnly(update, createdAt, Instant.now()), CatalogEventPO.class);
    }

    private static Query eventQuery(String eventId) {
        return Query.query(Criteria.where("eventId").is(eventId));
    }
}
