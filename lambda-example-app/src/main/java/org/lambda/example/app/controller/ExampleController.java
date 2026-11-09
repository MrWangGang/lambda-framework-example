package org.lambda.example.app.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.lambda.example.app.service.CacheService;
import org.lambda.example.app.service.CatalogService;
import org.lambda.example.app.entity.*;
import org.lambda.example.app.service.DelayedMessageService;
import org.lambda.example.app.service.VectorService;
import org.lambda.framework.repository.operation.Paged;
import org.springframework.core.env.Environment;
import org.lambda.framework.common.util.sample.JsonUtil;
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

    public ExampleController(CatalogService catalog, CacheService cache, DelayedMessageService messages,
                             VectorService vectors, Environment environment) {
        this.catalog = catalog;
        this.cache = cache;
        this.messages = messages;
        this.vectors = vectors;
        this.environment = environment;
    }

    public static record InfoRequestDTO() {}
    public static record InfoDTO(String application, String greeting, String replicaSet) {}
    public static record CachePutDTO(@NotBlank String key, @NotNull @Size(max = 10000) String value,
                                     @NotBlank @Pattern(regexp = "[0-9]{1,5}") String ttlSeconds) {}
    public static record CacheKeyDTO(@NotBlank String key) {}
    public static record VectorPutDTO(@NotNull UUID id,
                                      @NotNull @Size(min = 3, max = 3) List<@NotBlank String> vector,
                                      @NotBlank @Size(max = 64) String category) {}
    public static record VectorQueryDTO(@NotNull @Size(min = 3, max = 3) List<@NotBlank String> vector,
                                        @NotBlank @Pattern(regexp = "(?:[1-9][0-9]?|100)") String limit) {}

    @MessageMapping("/api/system/info")
    public Mono<InfoDTO> info(@Payload InfoRequestDTO request) {
        return Mono.just(new InfoDTO(environment.getRequiredProperty("spring.application.name"),
                environment.getRequiredProperty("example.greeting"),
                environment.getRequiredProperty("lambda.repository.mongo.replica")));
    }

    @MessageMapping("/api/product/create")
    public Mono<ProductEntity.ViewDTO> create(@Valid @Payload ProductEntity.SaveDTO input) {
        return catalog.create(input).map(ProductEntity.ViewDTO::from);
    }

    @MessageMapping("/api/product/find")
    public Mono<ProductEntity.ViewDTO> product(@Valid @Payload ProductEntity.IdDTO input) {
        return catalog.get(input.id()).map(ProductEntity.ViewDTO::from);
    }

    @MessageMapping("/api/product/cursor")
    public Mono<ProductEntity.CursorResultDTO> products(@Valid @Payload ProductEntity.CursorDTO input) {
        int limit = input.limit() == null ? 20 : Integer.parseInt(input.limit());
        return catalog.list(input.category(), limit, input.beforeTime(), input.beforeId());
    }

    @MessageMapping("/api/product/page")
    public Mono<Paged<ProductEntity.ViewDTO>> page(@Valid @Payload ProductEntity.PageDTO input) {
        return catalog.page(input);
    }

    @MessageMapping("/api/product/update")
    public Mono<ProductEntity.ViewDTO> update(@Valid @Payload ProductEntity.UpdateDTO input) {
        return catalog.update(input.id(), Long.parseLong(input.version()), input.product()).map(ProductEntity.ViewDTO::from);
    }

    @MessageMapping("/api/favorite/add")
    public Mono<Void> favorite(@Valid @Payload FavoriteEntity.ChangeDTO input) {
        return catalog.favorite(input.userId(), input.productId());
    }

    @MessageMapping("/api/favorite/remove")
    public Mono<Void> unfavorite(@Valid @Payload FavoriteEntity.ChangeDTO input) {
        return catalog.unfavorite(input.userId(), input.productId());
    }

    @MessageMapping("/api/favorite/list")
    public Flux<FavoriteEntity.ViewDTO> favorites(@Valid @Payload FavoriteEntity.QueryDTO input) {
        return catalog.favorites(input.userId()).map(FavoriteEntity.ViewDTO::from);
    }

    @MessageMapping("/api/purchase/create")
    public Mono<PurchaseEntity.ViewDTO> purchase(@Valid @Payload PurchaseEntity.CreateDTO input) {
        return catalog.purchase(input).map(PurchaseEntity.ViewDTO::from);
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
    public Mono<DeliveryReceiptEntity.MessageDTO> delayed(@Valid @Payload DeliveryReceiptEntity.SendDTO input) {
        return messages.send(input);
    }

    @MessageMapping("/api/message/findReceipt")
    public Mono<DeliveryReceiptEntity.ViewDTO> receipt(@Valid @Payload DeliveryReceiptEntity.IdDTO input) {
        return messages.receipt(input).map(DeliveryReceiptEntity.ViewDTO::from);
    }

    @MessageMapping("/api/vector/upsert")
    public Mono<JsonNode> upsertVector(@Valid @Payload VectorPutDTO input) {
        return vectors.upsert(input.id().toString(), vector(input.vector()), input.category()).map(ExampleController::readJson);
    }

    @MessageMapping("/api/vector/search")
    public Mono<JsonNode> queryVector(@Valid @Payload VectorQueryDTO input) {
        return vectors.query(vector(input.vector()), Integer.parseInt(input.limit())).map(ExampleController::readJson);
    }

    private static JsonNode readJson(String json) {
        return JsonUtil.stringToObj(json, JsonNode.class).orElseThrow();
    }

    private static List<Double> vector(List<String> values) {
        try {
            return values.stream().map(Double::parseDouble).toList();
        } catch (NumberFormatException error) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Vector values must be numeric strings");
        }
    }
}
