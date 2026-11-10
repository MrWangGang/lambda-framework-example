package com.lambda.example.servicea.repository;

import com.lambda.example.servicea.po.FavoritePO;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import jakarta.annotation.Resource;
import org.bson.types.ObjectId;
import org.lambda.framework.repository.operation.MongoAuditUpdates;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public class FavoriteRepository {
    @Resource
    private ReactiveMongoTemplate mongo;

    public Mono<UpdateResult> saveIfAbsent(ObjectId userId, ObjectId productId) {
        return Mono.defer(() -> {
            var now = Instant.now();
            var update = new Update().setOnInsert("userId", userId).setOnInsert("productId", productId);
            return mongo.upsert(byUserAndProduct(userId, productId),
                    MongoAuditUpdates.insertOnly(update, now, now), FavoritePO.class);
        });
    }

    public Mono<DeleteResult> delete(ObjectId userId, ObjectId productId) {
        return mongo.remove(byUserAndProduct(userId, productId), FavoritePO.class);
    }

    public Flux<FavoritePO> findRecent(ObjectId userId, int limit) {
        var query = Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(limit);
        return mongo.find(query, FavoritePO.class);
    }

    private static Query byUserAndProduct(ObjectId userId, ObjectId productId) {
        return Query.query(Criteria.where("userId").is(userId).and("productId").is(productId));
    }
}
