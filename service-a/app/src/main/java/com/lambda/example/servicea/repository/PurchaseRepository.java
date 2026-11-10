package com.lambda.example.servicea.repository;

import com.lambda.example.servicea.po.PurchasePO;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PurchaseRepository extends ReactiveMongoRepository<PurchasePO, ObjectId> {
    Mono<PurchasePO> findByRequestId(String requestId);

    @Aggregation(pipeline = {
            "{ $match: { userId: ?0 } }",
            "{ $lookup: { from: 'products', localField: 'productId', foreignField: '_id', as: 'product' } }",
            "{ $unwind: { path: '$product', preserveNullAndEmptyArrays: true } }",
            "{ $group: { _id: '$product.category', purchases: { $sum: 1 }, units: { $sum: '$quantity' } } }",
            "{ $sort: { _id: 1 } }",
            "{ $project: { _id: 0, category: '$_id', purchases: 1, units: 1 } }"
    })
    Flux<CategoryTotals> summarizeByCategory(ObjectId userId);

    record CategoryTotals(String category, long purchases, long units) {}
}
