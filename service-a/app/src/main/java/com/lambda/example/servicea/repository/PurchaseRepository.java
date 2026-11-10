package com.lambda.example.servicea.repository;

import com.lambda.example.servicea.contract.PurchaseContract.CategoryTotal;
import com.lambda.example.servicea.po.PurchasePO;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface PurchaseRepository extends ReactiveMongoRepository<PurchasePO, ObjectId> {
    @Aggregation(pipeline = {
            "{ $match: { userId: ?0 } }",
            "{ $lookup: { from: 'products', localField: 'productId', foreignField: '_id', as: 'product' } }",
            "{ $unwind: { path: '$product', preserveNullAndEmptyArrays: true } }",
            "{ $group: { _id: { $ifNull: ['$product.category', 'uncategorized'] }, purchases: { $sum: 1 }, units: { $sum: '$quantity' } } }",
            "{ $sort: { _id: 1 } }",
            "{ $project: { _id: 0, category: '$_id', purchases: { $toString: '$purchases' }, units: { $toString: '$units' } } }"
    })
    Flux<CategoryTotal> summarizeByCategory(ObjectId userId);
}
