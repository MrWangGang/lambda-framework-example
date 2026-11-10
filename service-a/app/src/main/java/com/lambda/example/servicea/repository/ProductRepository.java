package com.lambda.example.servicea.repository;

import org.bson.types.ObjectId;
import com.lambda.example.servicea.po.ProductPO;
import org.lambda.framework.repository.operation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

@Repository
public class ProductRepository implements ReactiveUnifyPagingRepositoryOperation<ProductPO> {
    private final ReactiveMongoTemplate mongo;

    public ProductRepository(ReactiveMongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Mono<ProductPO> findById(ObjectId id) {
        return mongo.findOne(Query.query(Criteria.where("_id").is(id)), ProductPO.class);
    }

    public Mono<ProductPO> insert(ProductPO product) {
        return mongo.insert(product);
    }

    public Flux<ProductPO> findWindow(String category, Optional<Cursor> cursor, int limit) {
        Criteria criteria = Criteria.where("category").is(category);
        cursor.ifPresent(position -> criteria.andOperator(new Criteria().orOperator(
                Criteria.where("createdAt").lt(position.time()),
                new Criteria().andOperator(Criteria.where("createdAt").is(position.time()),
                        Criteria.where("_id").lt(position.id())))));
        return mongo.find(Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(limit),
                ProductPO.class);
    }

    public Mono<ProductPO> updateFields(ProductPO product, long version) {
        var query = Query.query(Criteria.where("_id").is(product.getId()).and("version").is(version));
        var update = new Update().set("name", product.getName()).set("category", product.getCategory())
                .set("price", product.getPrice()).set("stock", product.getStock()).set("status", product.getStatus());
        return update(query, update);
    }

    public Mono<ProductPO> decrementStock(ObjectId productId, int quantity) {
        var query = Query.query(Criteria.where("_id").is(productId).and("stock").gte(quantity));
        return update(query, new Update().inc("stock", -quantity));
    }

    private Mono<ProductPO> update(Query query, Update update) {
        return mongo.findAndModify(query, MongoAuditUpdates.modified(update),
                FindAndModifyOptions.options().returnNew(true), ProductPO.class);
    }

    public record Cursor(Instant time, ObjectId id) {}

    public Mono<Long> countCategory(String category) {
        return mongo.count(Query.query(Criteria.where("category").is(category)), ProductPO.class);
    }

    public Mono<Paged<ProductPO>> page(String category, Paging paging) {
        return queryPage(paging, category, Sort.by(Sort.Direction.DESC, "createdAt", "_id"),
                new CountedPageQuery<String, ProductPO>() {
                    public Mono<Long> count(String category) {
                        return mongo.count(Query.query(Criteria.where("category").is(category)), ProductPO.class);
                    }
                    public Flux<ProductPO> query(String category, Pageable pageable) {
                        return mongo.find(Query.query(Criteria.where("category").is(category)).with(pageable), ProductPO.class);
                    }
                });
    }
}
