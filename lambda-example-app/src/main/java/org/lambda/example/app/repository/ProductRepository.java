package org.lambda.example.app.repository;

import org.bson.types.ObjectId;
import org.lambda.example.app.entity.ProductEntity;
import org.lambda.framework.repository.operation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class ProductRepository implements ReactiveUnifyPagingRepositoryOperation<ProductEntity> {
    private final ReactiveMongoTemplate mongo;

    public ProductRepository(ReactiveMongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Mono<ProductEntity> findById(ObjectId id) {
        return mongo.findOne(Query.query(Criteria.where("_id").is(id)), ProductEntity.class);
    }

    public Mono<Paged<ProductEntity>> page(ProductEntity.PageDTO input) {
        int page = input.page() == null ? 1 : Integer.parseInt(input.page());
        int size = input.size() == null ? 10 : Integer.parseInt(input.size());
        if (page < 1 || size < 1 || size > 100) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use page >= 1 and size 1..100"));
        }
        var paging = Paging.builder().page(page).size(size).build();
        return sqlPaging(paging, input.category(), Sort.by(Sort.Direction.DESC, "createdAt", "_id"),
                new UnifyPagingSqlOperation<String, ProductEntity>() {
                    public Mono<Long> count(String category) {
                        return mongo.count(Query.query(Criteria.where("category").is(category)), ProductEntity.class);
                    }
                    public Flux<ProductEntity> query(String category, Pageable pageable) {
                        return mongo.find(Query.query(Criteria.where("category").is(category)).with(pageable), ProductEntity.class);
                    }
                });
    }

    public static ObjectId objectId(String hex) {
        if (hex == null || !ObjectId.isValid(hex)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID must be a 24-character ObjectId hex string");
        }
        return new ObjectId(hex);
    }
}
