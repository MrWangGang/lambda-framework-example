package com.lambda.example.servicea.repository;

import com.lambda.example.servicea.po.DeliveryReceiptPO;
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
public class DeliveryReceiptRepository {
    @Resource
    private ReactiveMongoTemplate mongo;

    public Mono<DeliveryReceiptPO> findById(ObjectId id) {
        return mongo.findById(id, DeliveryReceiptPO.class);
    }

    public Mono<UpdateResult> saveIfAbsent(ObjectId id, String content, Instant createdAt, Instant receivedAt) {
        var query = Query.query(Criteria.where("_id").is(id));
        var receipt = new Update().setOnInsert("content", content).setOnInsert("receivedAt", receivedAt);
        return mongo.upsert(query, MongoAuditUpdates.insertOnly(receipt, createdAt, receivedAt), DeliveryReceiptPO.class);
    }
}
