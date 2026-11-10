package com.lambda.example.servicea.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.lambda.example.servicea.service.CacheService;
import com.lambda.example.servicea.service.CatalogService;
import com.lambda.example.servicea.contract.*;
import com.lambda.example.servicea.service.DelayedMessageService;
import com.lambda.example.servicea.service.VectorService;
import org.lambda.framework.common.support.UiFlux;
import org.lambda.framework.repository.operation.Paged;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.core.env.Environment;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Controller
public class ExampleController {
    private final CatalogService catalog;
    private final CacheService cache;
    private final DelayedMessageService messages;
    private final VectorService vectors;
    private final Environment environment;
    private final String replicaSet;

    public ExampleController(CatalogService catalog, CacheService cache, DelayedMessageService messages,
                             VectorService vectors, Environment environment, MongoConnectionDetails mongo) {
        this.catalog = catalog;
        this.cache = cache;
        this.messages = messages;
        this.vectors = vectors;
        this.environment = environment;
        this.replicaSet = mongo.getConnectionString().getRequiredReplicaSetName();
    }

    public static record InfoRequestDTO() {}
    public static record InfoDTO(String application, String greeting, String replicaSet) {}
    public static record CachePutDTO(@NotBlank String key, @NotNull @Size(max = 10000) String value,
                                     @NotBlank @Pattern(regexp = "[0-9]{1,5}") String ttlSeconds) {}
    public static record CacheKeyDTO(@NotBlank String key) {}
    public static record VectorPutDTO(@NotNull UUID id,
                                      @NotNull @Size(min = 3, max = 3) List<@NotNull Float> vector,
                                      @NotBlank @Size(max = 64) String category) {}
    public static record VectorQueryDTO(@NotNull @Size(min = 3, max = 3) List<@NotNull Float> vector,
                                        @NotBlank @Pattern(regexp = "(?:[1-9][0-9]?|100)") String limit) {}

    @MessageMapping("/api/system/info")
    public Mono<InfoDTO> info(@Payload InfoRequestDTO request) {
        return Mono.just(new InfoDTO(environment.getRequiredProperty("spring.application.name"),
                environment.getRequiredProperty("example.greeting"),
                replicaSet));
    }

    @MessageMapping("/api/product/create")
    public Mono<ProductContract.Detail> create(@Valid @Payload ProductContract.SaveRequest input) {
        return catalog.create(input).map(ProductContract.Detail::from);
    }

    @MessageMapping("/api/product/find")
    public Mono<ProductContract.Detail> product(@Valid @Payload ProductContract.FindRequest input) {
        return catalog.get(input.id()).map(ProductContract.Detail::from);
    }

    @MessageMapping("/api/product/cursor")
    public Mono<ProductContract.CursorResult> products(@Valid @Payload ProductContract.CursorRequest input) {
        int limit = input.limit() == null ? 20 : Integer.parseInt(input.limit());
        return catalog.list(input.category(), limit, input.beforeTime(), input.beforeId());
    }

    @MessageMapping("/api/product/page")
    public Mono<Paged<ProductContract.Detail>> page(@Valid @Payload ProductContract.PageRequest input) {
        return catalog.page(input);
    }

    @MessageMapping("/api/product/export")
    public UiFlux<ProductContract.Detail> export(@Valid @Payload ProductContract.CursorRequest input) {
        return UiFlux.of(products(input).flatMapMany(page -> Flux.fromIterable(page.items())));
    }

    @MessageMapping("/api/product/update")
    public Mono<ProductContract.Detail> update(@Valid @Payload ProductContract.UpdateRequest input) {
        return catalog.update(input.id(), Long.parseLong(input.version()), input.product()).map(ProductContract.Detail::from);
    }

    @MessageMapping("/api/favorite/add")
    public Mono<Void> favorite(@Valid @Payload FavoriteContract.ChangeRequest input) {
        return catalog.favorite(input.userId(), input.productId());
    }

    @MessageMapping("/api/favorite/remove")
    public Mono<Void> unfavorite(@Valid @Payload FavoriteContract.ChangeRequest input) {
        return catalog.unfavorite(input.userId(), input.productId());
    }

    @MessageMapping("/api/favorite/list")
    public Flux<FavoriteContract.Detail> favorites(@Valid @Payload FavoriteContract.QueryRequest input) {
        return catalog.favorites(input.userId()).map(FavoriteContract.Detail::from);
    }

    @MessageMapping("/api/purchase/create")
    public Mono<PurchaseContract.Detail> purchase(@Valid @Payload PurchaseContract.CreateRequest input) {
        return catalog.purchase(input).map(PurchaseContract.Detail::from);
    }

    @MessageMapping("/api/purchase/summary")
    public Flux<PurchaseContract.CategoryTotal> summarizePurchases(@Valid @Payload PurchaseContract.SummaryRequest input) {
        return catalog.summarizePurchases(input.userId());
    }

    @MessageMapping("/api/cache/put")
    public Mono<Boolean> putCache(@Valid @Payload CachePutDTO input) {
        return cache.put(input.key(), input.value(), Long.parseLong(input.ttlSeconds()));
    }

    @MessageMapping("/api/cache/find")
    public Mono<String> getCache(@Valid @Payload CacheKeyDTO input) {
        return cache.get(input.key());
    }

    @MessageMapping("/api/cache/remove")
    public Mono<Long> deleteCache(@Valid @Payload CacheKeyDTO input) {
        return cache.delete(input.key());
    }

    @MessageMapping("/api/message/sendDelayed")
    public Mono<DeliveryReceiptContract.Message> delayed(@Valid @Payload DeliveryReceiptContract.SendRequest input) {
        return messages.send(input);
    }

    @MessageMapping("/api/message/findReceipt")
    public Mono<DeliveryReceiptContract.Detail> receipt(@Valid @Payload DeliveryReceiptContract.FindRequest input) {
        return messages.receipt(input.id()).map(DeliveryReceiptContract.Detail::from);
    }

    @MessageMapping("/api/vector/upsert")
    public Mono<Boolean> upsertVector(@Valid @Payload VectorPutDTO input) {
        return vectors.upsert(input.id(), input.vector(), input.category());
    }

    @MessageMapping("/api/vector/search")
    public Flux<VectorService.VectorHit> queryVector(@Valid @Payload VectorQueryDTO input) {
        return vectors.query(input.vector(), Integer.parseInt(input.limit()));
    }
}
