package com.lambda.example.servicea.repository;

import com.lambda.example.servicea.contract.ProductContract;
import org.bson.types.ObjectId;
import com.lambda.example.servicea.po.ProductPO;
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
public class ProductRepository implements ReactiveUnifyPagingRepositoryOperation<ProductPO> {
    private final ReactiveMongoTemplate mongo;

    public ProductRepository(ReactiveMongoTemplate mongo) {
        this.mongo = mongo;
    }

    public Mono<ProductPO> findById(ObjectId id) {
        return mongo.findOne(Query.query(Criteria.where("_id").is(id)), ProductPO.class);
    }

    public Mono<Long> countCategory(String category) {
        return mongo.count(Query.query(Criteria.where("category").is(category)), ProductPO.class);
    }

    public Mono<Paged<ProductPO>> page(ProductContract.PageRequest input) {
        int page = input.page() == null ? 1 : Integer.parseInt(input.page());
        int size = input.size() == null ? 10 : Integer.parseInt(input.size());
        if (page < 1 || size < 1 || size > 100) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use page >= 1 and size 1..100"));
        }
        var paging = Paging.builder().page(page).size(size).build();
        return queryPage(paging, input.category(), Sort.by(Sort.Direction.DESC, "createdAt", "_id"),
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
