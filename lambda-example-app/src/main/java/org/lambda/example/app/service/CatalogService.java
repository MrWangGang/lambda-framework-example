package org.lambda.example.app.service;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
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
import org.lambda.example.app.entity.FavoriteEntity;
import org.lambda.example.app.entity.ProductEntity;
import org.lambda.example.app.entity.PurchaseEntity;
import org.lambda.example.app.repository.ProductRepository;
import org.lambda.framework.repository.operation.Paged;

@Service
public class CatalogService {
    private final ReactiveMongoTemplate mongo;
    private final TransactionalOperator transactions;
    private final ProductRepository products;

    public CatalogService(ReactiveMongoTemplate mongo, TransactionalOperator transactions, ProductRepository products) {
        this.mongo = mongo;
        this.transactions = transactions;
        this.products = products;
    }
    @Bean
    ApplicationRunner catalogIndexes() {
        return args -> Mono.when(
            mongo.indexOps(PurchaseEntity.class).createIndex(new Index().on("requestId", Sort.Direction.ASC)
                    .unique().named("unique_purchase_request")),
            mongo.indexOps(ProductEntity.class).createIndex(new Index().on("category", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC).on("_id", Sort.Direction.DESC).named("category_recent")),
            mongo.indexOps(FavoriteEntity.class).createIndex(new Index().on("userId", Sort.Direction.ASC)
                    .on("productId", Sort.Direction.ASC).unique().named("unique_user_product")),
            mongo.indexOps(FavoriteEntity.class).createIndex(new Index().on("userId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC).on("_id", Sort.Direction.DESC).named("user_recent")),
            mongo.indexOps(FavoriteEntity.class).createIndex(new Index().on("productId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC).named("product_recent"))
        ).block(Duration.ofSeconds(30));
    }
    public Mono<ProductEntity> create(ProductEntity.SaveDTO input) {
        return mongo.insert(new ProductEntity(new ObjectId(), input.name(), input.category(),
                input.price(), Integer.parseInt(input.stock()), null, Instant.now(), input.status()));
    }
    public Mono<ProductEntity> get(ObjectId id) {
        return products.findById(id).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")));
    }
    public Mono<ProductEntity.CursorResultDTO> list(String category, int limit, Instant beforeTime, ObjectId beforeId) {
        if (limit < 1 || limit > 100 || category == null || category.isBlank()
                || (beforeTime == null) != (beforeId == null)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use category, limit 1..100, and both cursor fields"));
        }
        Criteria criteria = Criteria.where("category").is(category);
        if (beforeTime != null) criteria = criteria.andOperator(new Criteria().orOperator(
                Criteria.where("createdAt").lt(beforeTime),
                new Criteria().andOperator(Criteria.where("createdAt").is(beforeTime), Criteria.where("_id").lt(beforeId))));
        Query query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(limit + 1);
        return mongo.find(query, ProductEntity.class).collectList().map(rows -> {
            boolean more = rows.size() > limit;
            var items = more ? rows.subList(0, limit) : rows;
            var last = items.isEmpty() ? null : items.getLast();
            return new ProductEntity.CursorResultDTO(items.stream().map(ProductEntity.ViewDTO::from).toList(),
                    more ? last.createdAt() : null, more ? last.id() : null);
        });
    }
    public Mono<ProductEntity> update(ObjectId id, long version, ProductEntity.SaveDTO input) {
        Query query = Query.query(Criteria.where("_id").is(id).and("version").is(version));
        Update update = new Update().set("name", input.name()).set("category", input.category())
                .set("price", input.price()).set("stock", Integer.parseInt(input.stock()))
                .set("status", input.status()).inc("version", 1);
        return mongo.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), ProductEntity.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Product version changed or product missing")));
    }
    public Mono<Void> favorite(ObjectId userId, ObjectId productId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("productId").is(productId));
        Update update = new Update().setOnInsert("userId", userId).setOnInsert("productId", productId)
                .setOnInsert("createdAt", Instant.now());
        return get(productId).then(mongo.upsert(query, update, FavoriteEntity.class))
                .onErrorResume(DuplicateKeyException.class, error -> Mono.empty()).then();
    }
    public Mono<Void> unfavorite(ObjectId userId, ObjectId productId) {
        return mongo.remove(Query.query(Criteria.where("userId").is(userId).and("productId").is(productId)), FavoriteEntity.class).then();
    }
    public Flux<FavoriteEntity> favorites(ObjectId userId) {
        return mongo.find(Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(50), FavoriteEntity.class);
    }
    public Mono<PurchaseEntity> purchase(PurchaseEntity.CreateDTO input) {
        return findPurchase(input.requestId()).flatMap(existing -> verifyRequest(existing, input))
                .switchIfEmpty(Mono.defer(() -> {
                    Query available = Query.query(Criteria.where("_id").is(input.productId()).and("stock").gte(Integer.parseInt(input.quantity())));
                    return mongo.findAndModify(available, new Update().inc("stock", -Integer.parseInt(input.quantity())).inc("version", 1),
                                    FindAndModifyOptions.options().returnNew(true), ProductEntity.class)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Product unavailable or insufficient stock")))
                            .flatMap(product -> mongo.insert(new PurchaseEntity(new ObjectId(), input.requestId(), input.userId(), product.id(),
                                    Integer.parseInt(input.quantity()), product.price(), Instant.now())))
                            .as(transactions::transactional)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(25))
                                    .filter(CatalogService::transientTransactionFailure)
                                    .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
                })).onErrorResume(DuplicateKeyException.class, error -> findPurchase(input.requestId())
                        .switchIfEmpty(Mono.error(error)).flatMap(existing -> verifyRequest(existing, input)));
    }
    public Mono<Paged<ProductEntity.ViewDTO>> page(ProductEntity.PageDTO input) {
        return products.page(input).map(page -> Paged.<ProductEntity.ViewDTO>builder()
                .page(page.getPage()).size(page.getSize()).total(page.getTotal()).pages(page.getPages())
                .records(page.getRecords().stream().map(ProductEntity.ViewDTO::from).toList()).build());
    }

    private Mono<PurchaseEntity> findPurchase(String requestId) {
        return mongo.findOne(Query.query(Criteria.where("requestId").is(requestId)), PurchaseEntity.class);
    }

    private static boolean transientTransactionFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof MongoException mongoFailure
                    && mongoFailure.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) return true;
        }
        return false;
    }
    private Mono<PurchaseEntity> verifyRequest(PurchaseEntity existing, PurchaseEntity.CreateDTO input) {
        if (!existing.userId().equals(input.userId()) || !existing.productId().equals(input.productId()) || existing.quantity() != Integer.parseInt(input.quantity()))
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "requestId belongs to a different purchase"));
        return Mono.just(existing);
    }
}
