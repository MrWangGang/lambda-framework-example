package org.lambda.example.app.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.lambda.example.app.cache.CacheService;
import org.lambda.example.app.catalog.CatalogService;
import org.lambda.example.app.messaging.DelayedMessageService;
import org.lambda.example.app.vector.VectorService;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.lambda.example.app.catalog.Models.*;

@RestController
@RequestMapping("/api")
public class ExampleController {
    private final CatalogService catalog;
    private final CacheService cache;
    private final DelayedMessageService messages;
    private final VectorService vectors;
    private final Environment environment;
    public ExampleController(CatalogService catalog, CacheService cache, DelayedMessageService messages,
                             VectorService vectors, Environment environment) {
        this.catalog=catalog; this.cache=cache; this.messages=messages; this.vectors=vectors; this.environment=environment;
    }
    @GetMapping("/info")
    public Map<String,String> info() {
        return Map.of("application", environment.getRequiredProperty("spring.application.name"),
                "greeting", environment.getRequiredProperty("example.greeting"),
                "replicaSet", environment.getRequiredProperty("lambda.repository.mongo.replica"));
    }
    @PostMapping("/products")
    public Mono<Product> create(@Valid @RequestBody ProductInput input) { return catalog.create(input); }
    @GetMapping("/products/{id}")
    public Mono<Product> product(@PathVariable String id) { return catalog.get(id); }
    @GetMapping("/products")
    public Mono<ProductPage> products(@RequestParam String category, @RequestParam(defaultValue="20") int limit,
                                     @RequestParam(required=false) Instant beforeTime, @RequestParam(required=false) String beforeId) {
        return catalog.list(category, limit, beforeTime, beforeId);
    }
    @PutMapping("/products/{id}")
    public Mono<Product> update(@PathVariable String id, @RequestParam long version, @Valid @RequestBody ProductInput input) {
        return catalog.update(id, version, input);
    }
    @PutMapping("/users/{userId}/favorites/{productId}")
    public Mono<Void> favorite(@PathVariable String userId, @PathVariable String productId) { return catalog.favorite(userId, productId); }
    @DeleteMapping("/users/{userId}/favorites/{productId}")
    public Mono<Void> unfavorite(@PathVariable String userId, @PathVariable String productId) { return catalog.unfavorite(userId, productId); }
    @GetMapping("/users/{userId}/favorites")
    public Flux<Favorite> favorites(@PathVariable String userId) { return catalog.favorites(userId); }
    @PostMapping("/purchases")
    public Mono<Purchase> purchase(@Valid @RequestBody PurchaseInput input) { return catalog.purchase(input); }
    public record CacheInput(@NotNull @Size(max=10000) String value, @Min(1) @Max(86400) long ttlSeconds) {}
    @PutMapping("/cache/{key}")
    public Mono<Boolean> putCache(@PathVariable String key, @Valid @RequestBody CacheInput input) { return cache.put(key, input.value(), input.ttlSeconds()); }
    @GetMapping(value="/cache/{key}", produces=MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> getCache(@PathVariable String key) { return cache.get(key); }
    @DeleteMapping("/cache/{key}")
    public Mono<Long> deleteCache(@PathVariable String key) { return cache.delete(key); }
    public record DelayInput(@NotBlank @Size(max=10000) String content, @Min(0) @Max(60000) long delayMillis) {}
    @PostMapping("/messages/delayed")
    public Mono<DelayedMessageService.Message> delayed(@Valid @RequestBody DelayInput input) { return messages.send(input.content(), input.delayMillis()); }
    @GetMapping("/messages/{id}")
    public Mono<org.bson.Document> receipt(@PathVariable String id) { return messages.receipt(id); }
    public record VectorInput(@NotNull @Size(min=3,max=3) List<@NotNull Double> vector, @NotBlank @Size(max=64) String category) {}
    public record VectorQuery(@NotNull @Size(min=3,max=3) List<@NotNull Double> vector, @Min(1) @Max(100) int limit) {}
    @PutMapping(value="/vectors/{id}", produces=MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> upsertVector(@PathVariable UUID id, @Valid @RequestBody VectorInput input) { return vectors.upsert(id.toString(), input.vector(), input.category()); }
    @PostMapping(value="/vectors/query", produces=MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> queryVector(@Valid @RequestBody VectorQuery input) { return vectors.query(input.vector(), input.limit()); }
}
