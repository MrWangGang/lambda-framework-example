package org.lambda.example.app.catalog;

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
import java.util.UUID;
import static org.lambda.example.app.catalog.Models.*;

@Service
public class CatalogService {
    private final ReactiveMongoTemplate mongo;
    private final TransactionalOperator transactions;
    public CatalogService(ReactiveMongoTemplate mongo, TransactionalOperator transactions) {
        this.mongo = mongo;
        this.transactions = transactions;
    }
    @Bean
    ApplicationRunner catalogIndexes() {
        return args -> Mono.when(
            mongo.indexOps(Product.class).createIndex(new Index().on("category", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC).on("_id", Sort.Direction.DESC).named("category_recent")),
            mongo.indexOps(Favorite.class).createIndex(new Index().on("userId", Sort.Direction.ASC)
                    .on("productId", Sort.Direction.ASC).unique().named("unique_user_product")),
            mongo.indexOps(Favorite.class).createIndex(new Index().on("userId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC).on("_id", Sort.Direction.DESC).named("user_recent")),
            mongo.indexOps(Favorite.class).createIndex(new Index().on("productId", Sort.Direction.ASC)
                    .on("createdAt", Sort.Direction.DESC).named("product_recent"))
        ).block(Duration.ofSeconds(30));
    }
    public Mono<Product> create(ProductInput input) {
        return mongo.insert(new Product(UUID.randomUUID().toString(), input.name(), input.category(),
                input.price(), input.stock(), null, Instant.now()));
    }
    public Mono<Product> get(String id) {
        return mongo.findById(id, Product.class).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")));
    }
    public Mono<ProductPage> list(String category, int limit, Instant beforeTime, String beforeId) {
        if (limit < 1 || limit > 100 || category == null || category.isBlank()
                || (beforeTime == null) != (beforeId == null)) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use category, limit 1..100, and both cursor fields"));
        }
        Criteria criteria = Criteria.where("category").is(category);
        if (beforeTime != null) criteria = criteria.andOperator(new Criteria().orOperator(
                Criteria.where("createdAt").lt(beforeTime),
                new Criteria().andOperator(Criteria.where("createdAt").is(beforeTime), Criteria.where("_id").lt(beforeId))));
        Query query = Query.query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(limit + 1);
        return mongo.find(query, Product.class).collectList().map(rows -> {
            boolean more = rows.size() > limit;
            var items = more ? rows.subList(0, limit) : rows;
            var last = items.isEmpty() ? null : items.getLast();
            return new ProductPage(items, more ? last.createdAt() : null, more ? last.id() : null);
        });
    }
    public Mono<Product> update(String id, long version, ProductInput input) {
        Query query = Query.query(Criteria.where("_id").is(id).and("version").is(version));
        Update update = new Update().set("name", input.name()).set("category", input.category())
                .set("price", input.price()).set("stock", input.stock()).inc("version", 1);
        return mongo.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Product.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Product version changed or product missing")));
    }
    public Mono<Void> favorite(String userId, String productId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("productId").is(productId));
        Update update = new Update().setOnInsert("userId", userId).setOnInsert("productId", productId)
                .setOnInsert("createdAt", Instant.now());
        return get(productId).then(mongo.upsert(query, update, Favorite.class))
                .onErrorResume(DuplicateKeyException.class, error -> Mono.empty()).then();
    }
    public Mono<Void> unfavorite(String userId, String productId) {
        return mongo.remove(Query.query(Criteria.where("userId").is(userId).and("productId").is(productId)), Favorite.class).then();
    }
    public Flux<Favorite> favorites(String userId) {
        return mongo.find(Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(50), Favorite.class);
    }
    public Mono<Purchase> purchase(PurchaseInput input) {
        return mongo.findById(input.requestId(), Purchase.class).flatMap(existing -> verifyRequest(existing, input))
                .switchIfEmpty(Mono.defer(() -> {
                    Query available = Query.query(Criteria.where("_id").is(input.productId()).and("stock").gte(input.quantity()));
                    return mongo.findAndModify(available, new Update().inc("stock", -input.quantity()).inc("version", 1),
                                    FindAndModifyOptions.options().returnNew(true), Product.class)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Product unavailable or insufficient stock")))
                            .flatMap(product -> mongo.insert(new Purchase(input.requestId(), input.userId(), product.id(),
                                    input.quantity(), product.price(), Instant.now())))
                            .as(transactions::transactional)
                            .retryWhen(Retry.backoff(3, Duration.ofMillis(25))
                                    .filter(CatalogService::transientTransactionFailure)
                                    .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
                })).onErrorResume(DuplicateKeyException.class, error -> mongo.findById(input.requestId(), Purchase.class)
                        .switchIfEmpty(Mono.error(error)).flatMap(existing -> verifyRequest(existing, input)));
    }
    private static boolean transientTransactionFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof MongoException mongoFailure
                    && mongoFailure.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) return true;
        }
        return false;
    }
    private Mono<Purchase> verifyRequest(Purchase existing, PurchaseInput input) {
        if (!existing.userId().equals(input.userId()) || !existing.productId().equals(input.productId()) || existing.quantity() != input.quantity())
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "requestId belongs to a different purchase"));
        return Mono.just(existing);
    }
}
