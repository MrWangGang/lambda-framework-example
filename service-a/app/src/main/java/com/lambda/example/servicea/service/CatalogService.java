package com.lambda.example.servicea.service;

import com.lambda.example.servicea.contract.PurchaseContract;
import com.lambda.example.servicea.contract.ProductContract;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import com.mongodb.MongoException;
import java.time.Duration;
import java.time.Instant;
import org.bson.types.ObjectId;
import com.lambda.example.servicea.po.FavoritePO;
import com.lambda.example.servicea.po.ProductPO;
import com.lambda.example.servicea.po.PurchasePO;
import com.lambda.example.servicea.repository.ProductRepository;
import com.lambda.example.servicea.repository.PurchaseRepository;
import org.lambda.framework.repository.operation.MongoAuditUpdates;
import org.lambda.framework.repository.operation.Paged;

@Service
public class CatalogService {
    private final ReactiveMongoTemplate mongo;
    private final TransactionalOperator transactions;
    private final ProductRepository products;
    private final PurchaseRepository purchases;

    public CatalogService(ReactiveMongoTemplate mongo, TransactionalOperator transactions, ProductRepository products,
                          PurchaseRepository purchases) {
        this.mongo = mongo;
        this.transactions = transactions;
        this.products = products;
        this.purchases = purchases;
    }
    public Mono<ProductPO> create(ProductContract.SaveRequest input) {
        return mongo.insert(new ProductPO(new ObjectId(), input.name(), input.category(),
                input.price(), Integer.parseInt(input.stock()), input.status()));
    }
    public Mono<ProductPO> get(ObjectId id) {
        return products.findById(id).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")));
    }
    public Mono<ProductContract.CursorResult> list(String category, int limit, Instant beforeTime, ObjectId beforeId) {
        if (limit < 1 || limit > 100 || category == null || category.isBlank()
                || (beforeTime == null) != (beforeId == null)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use category, limit 1..100, and both cursor fields"));
        }
        Criteria criteria = Criteria.where("category").is(category);
        if (beforeTime != null) criteria = criteria.andOperator(new Criteria().orOperator(
                Criteria.where("createdAt").lt(beforeTime),
                new Criteria().andOperator(Criteria.where("createdAt").is(beforeTime), Criteria.where("_id").lt(beforeId))));
        Query query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(limit + 1);
        return mongo.find(query, ProductPO.class).collectList().map(rows -> {
            boolean more = rows.size() > limit;
            var items = more ? rows.subList(0, limit) : rows;
            var last = items.isEmpty() ? null : items.getLast();
            return new ProductContract.CursorResult(items.stream().map(ProductContract.Detail::from).toList(),
                    more ? last.getCreatedAt() : null, more ? last.getId() : null);
        });
    }
    public Mono<ProductPO> update(ObjectId id, long version, ProductContract.SaveRequest input) {
        Query query = Query.query(Criteria.where("_id").is(id).and("version").is(version));
        Update update = new Update().set("name", input.name()).set("category", input.category())
                .set("price", input.price()).set("stock", Integer.parseInt(input.stock()))
                .set("status", input.status());
        return mongo.findAndModify(query, MongoAuditUpdates.modified(update), FindAndModifyOptions.options().returnNew(true), ProductPO.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Product version changed or product missing")));
    }
    public Mono<Void> favorite(ObjectId userId, ObjectId productId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("productId").is(productId));
        return get(productId).then(Mono.defer(() -> {
            Instant now = Instant.now();
            Update update = new Update().setOnInsert("userId", userId).setOnInsert("productId", productId);
            return mongo.upsert(query, MongoAuditUpdates.insertOnly(update, now, now), FavoritePO.class);
        })).onErrorResume(DuplicateKeyException.class, error -> Mono.empty()).then();
    }
    public Mono<Void> unfavorite(ObjectId userId, ObjectId productId) {
        return mongo.remove(Query.query(Criteria.where("userId").is(userId).and("productId").is(productId)), FavoritePO.class).then();
    }
    public Flux<FavoritePO> favorites(ObjectId userId) {
        return mongo.find(Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(50), FavoritePO.class);
    }
    public Mono<PurchasePO> purchase(PurchaseContract.CreateRequest input) {
        return findPurchase(input.requestId()).flatMap(existing -> verifyRequest(existing, input))
                .switchIfEmpty(Mono.defer(() -> {
                    int quantity = Integer.parseInt(input.quantity());
                    Query available = Query.query(Criteria.where("_id").is(input.productId()).and("stock").gte(quantity));
                    Update decrement = MongoAuditUpdates.modified(new Update().inc("stock", -quantity));
                    return mongo.findAndModify(available, decrement,
                                    FindAndModifyOptions.options().returnNew(true), ProductPO.class)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Product unavailable or insufficient stock")))
                            .flatMap(product -> mongo.insert(new PurchasePO(new ObjectId(), input.requestId(), input.userId(), product.getId(),
                                    quantity, product.getPrice())))
                            .as(transactions::transactional)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(25))
                                    .filter(CatalogService::transientTransactionFailure)
                                    .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
                })).onErrorResume(DuplicateKeyException.class, error -> findPurchase(input.requestId())
                        .switchIfEmpty(Mono.error(error)).flatMap(existing -> verifyRequest(existing, input)));
    }
    public Flux<PurchaseContract.CategoryTotal> summarizePurchases(ObjectId userId) {
        return purchases.summarizeByCategory(userId);
    }

    public Mono<Paged<ProductContract.Detail>> page(ProductContract.PageRequest input) {
        return products.page(input).map(page -> Paged.<ProductContract.Detail>builder()
                .page(page.getPage()).size(page.getSize()).total(page.getTotal()).pages(page.getPages())
                .records(page.getRecords().stream().map(ProductContract.Detail::from).toList()).build());
    }

    private Mono<PurchasePO> findPurchase(String requestId) {
        return mongo.findOne(Query.query(Criteria.where("requestId").is(requestId)), PurchasePO.class);
    }

    private static boolean transientTransactionFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof MongoException mongoFailure
                    && mongoFailure.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) return true;
        }
        return false;
    }
    private Mono<PurchasePO> verifyRequest(PurchasePO existing, PurchaseContract.CreateRequest input) {
        if (!existing.getUserId().equals(input.userId()) || !existing.getProductId().equals(input.productId()) || existing.getQuantity() != Integer.parseInt(input.quantity()))
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "requestId belongs to a different purchase"));
        return Mono.just(existing);
    }
}
