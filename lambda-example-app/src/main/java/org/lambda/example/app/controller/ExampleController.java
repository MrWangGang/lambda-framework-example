package org.lambda.example.app.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.lambda.example.app.service.CacheService;
import org.lambda.example.app.service.CatalogService;
import org.lambda.example.app.entity.*;
import org.lambda.example.app.service.DelayedMessageService;
import org.lambda.example.app.service.VectorService;
import org.lambda.framework.repository.operation.Paged;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

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

    @PostMapping("/system/info")
    public InfoDTO info(@RequestBody InfoRequestDTO request) {
        return new InfoDTO(environment.getRequiredProperty("spring.application.name"),
                environment.getRequiredProperty("example.greeting"),
                environment.getRequiredProperty("lambda.repository.mongo.replica"));
    }

    @PostMapping("/product/create")
    public Mono<ProductEntity.ViewDTO> create(@Valid @RequestBody ProductEntity.SaveDTO input) {
        return catalog.create(input).map(ProductEntity.ViewDTO::from);
    }

    @PostMapping("/product/find")
    public Mono<ProductEntity.ViewDTO> product(@Valid @RequestBody ProductEntity.IdDTO input) {
        return catalog.get(input.id()).map(ProductEntity.ViewDTO::from);
    }

    @PostMapping("/product/cursor")
    public Mono<ProductEntity.CursorResultDTO> products(@Valid @RequestBody ProductEntity.CursorDTO input) {
        int limit = input.limit() == null ? 20 : Integer.parseInt(input.limit());
        return catalog.list(input.category(), limit, input.beforeTime(), input.beforeId());
    }

    @PostMapping("/product/page")
    public Mono<Paged<ProductEntity.ViewDTO>> page(@Valid @RequestBody ProductEntity.PageDTO input) {
        return catalog.page(input);
    }

    @PostMapping("/product/update")
    public Mono<ProductEntity.ViewDTO> update(@Valid @RequestBody ProductEntity.UpdateDTO input) {
        return catalog.update(input.id(), Long.parseLong(input.version()), input.product()).map(ProductEntity.ViewDTO::from);
    }

    @PostMapping("/favorite/add")
    public Mono<Void> favorite(@Valid @RequestBody FavoriteEntity.ChangeDTO input) {
        return catalog.favorite(input.userId(), input.productId());
    }

    @PostMapping("/favorite/remove")
    public Mono<Void> unfavorite(@Valid @RequestBody FavoriteEntity.ChangeDTO input) {
        return catalog.unfavorite(input.userId(), input.productId());
    }

    @PostMapping("/favorite/list")
    public Flux<FavoriteEntity.ViewDTO> favorites(@Valid @RequestBody FavoriteEntity.QueryDTO input) {
        return catalog.favorites(input.userId()).map(FavoriteEntity.ViewDTO::from);
    }

    @PostMapping("/purchase/create")
    public Mono<PurchaseEntity.ViewDTO> purchase(@Valid @RequestBody PurchaseEntity.CreateDTO input) {
        return catalog.purchase(input).map(PurchaseEntity.ViewDTO::from);
    }

    @PostMapping("/cache/put")
    public Mono<Boolean> putCache(@Valid @RequestBody CachePutDTO input) {
        return cache.put(input.key(), input.value(), Long.parseLong(input.ttlSeconds()));
    }

    @PostMapping(value = "/cache/find", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> getCache(@Valid @RequestBody CacheKeyDTO input) {
        return cache.get(input.key());
    }

    @PostMapping("/cache/remove")
    public Mono<Long> deleteCache(@Valid @RequestBody CacheKeyDTO input) {
        return cache.delete(input.key());
    }

    @PostMapping("/message/sendDelayed")
    public Mono<DeliveryReceiptEntity.MessageDTO> delayed(@Valid @RequestBody DeliveryReceiptEntity.SendDTO input) {
        return messages.send(input);
    }

    @PostMapping("/message/findReceipt")
    public Mono<DeliveryReceiptEntity.ViewDTO> receipt(@Valid @RequestBody DeliveryReceiptEntity.IdDTO input) {
        return messages.receipt(input).map(DeliveryReceiptEntity.ViewDTO::from);
    }

    @PostMapping(value = "/vector/upsert", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> upsertVector(@Valid @RequestBody VectorPutDTO input) {
        return vectors.upsert(input.id().toString(), vector(input.vector()), input.category());
    }

    @PostMapping(value = "/vector/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> queryVector(@Valid @RequestBody VectorQueryDTO input) {
        return vectors.query(vector(input.vector()), Integer.parseInt(input.limit()));
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
