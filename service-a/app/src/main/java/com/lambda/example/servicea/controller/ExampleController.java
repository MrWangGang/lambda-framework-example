package com.lambda.example.servicea.controller;

import com.lambda.example.servicea.contract.CacheContract;
import com.lambda.example.servicea.contract.DeliveryReceiptContract;
import com.lambda.example.servicea.contract.FavoriteContract;
import com.lambda.example.servicea.contract.ProductContract;
import com.lambda.example.servicea.contract.PurchaseContract;
import com.lambda.example.servicea.contract.SystemContract;
import com.lambda.example.servicea.contract.VectorContract;
import com.lambda.example.servicea.service.CacheServiceImpl;
import com.lambda.example.servicea.service.CatalogServiceImpl;
import com.lambda.example.servicea.service.DelayedMessageServiceImpl;
import com.lambda.example.servicea.service.SystemServiceImpl;
import com.lambda.example.servicea.service.VectorServiceImpl;
import jakarta.annotation.Resource;
import org.lambda.framework.common.support.UiFlux;
import org.lambda.framework.repository.operation.Paged;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class ExampleController {
    @Resource
    private CatalogServiceImpl catalog;
    @Resource
    private CacheServiceImpl cache;
    @Resource
    private DelayedMessageServiceImpl messages;
    @Resource
    private VectorServiceImpl vectors;
    @Resource
    private SystemServiceImpl system;

    @MessageMapping("/api/system/info")
    public Mono<SystemContract.Info> info(@Payload SystemContract.EmptyRequest input) {
        return system.info(input);
    }

    @MessageMapping("/api/product/create")
    public Mono<ProductContract.Detail> create(@Payload ProductContract.SaveRequest input) {
        return catalog.create(input);
    }

    @MessageMapping("/api/product/find")
    public Mono<ProductContract.Detail> product(@Payload ProductContract.FindRequest input) {
        return catalog.find(input);
    }

    @MessageMapping("/api/product/cursor")
    public Mono<ProductContract.CursorResult> products(@Payload ProductContract.CursorRequest input) {
        return catalog.list(input);
    }

    @MessageMapping("/api/product/page")
    public Mono<Paged<ProductContract.Detail>> page(@Payload ProductContract.PageRequest input) {
        return catalog.page(input);
    }

    @MessageMapping("/api/product/export")
    public UiFlux<ProductContract.Detail> export(@Payload ProductContract.CursorRequest input) {
        return catalog.export(input);
    }

    @MessageMapping("/api/product/update")
    public Mono<ProductContract.Detail> update(@Payload ProductContract.UpdateRequest input) {
        return catalog.update(input);
    }

    @MessageMapping("/api/favorite/add")
    public Mono<Void> favorite(@Payload FavoriteContract.ChangeRequest input) {
        return catalog.favorite(input);
    }

    @MessageMapping("/api/favorite/remove")
    public Mono<Void> unfavorite(@Payload FavoriteContract.ChangeRequest input) {
        return catalog.unfavorite(input);
    }

    @MessageMapping("/api/favorite/list")
    public Flux<FavoriteContract.Detail> favorites(@Payload FavoriteContract.QueryRequest input) {
        return catalog.favorites(input);
    }

    @MessageMapping("/api/purchase/create")
    public Mono<PurchaseContract.Detail> purchase(@Payload PurchaseContract.CreateRequest input) {
        return catalog.purchase(input);
    }

    @MessageMapping("/api/purchase/summary")
    public Flux<PurchaseContract.CategoryTotal> summarizePurchases(@Payload PurchaseContract.SummaryRequest input) {
        return catalog.summarizePurchases(input);
    }

    @MessageMapping("/api/cache/put")
    public Mono<Boolean> putCache(@Payload CacheContract.PutRequest input) {
        return cache.put(input);
    }

    @MessageMapping("/api/cache/find")
    public Mono<String> getCache(@Payload CacheContract.KeyRequest input) {
        return cache.get(input);
    }

    @MessageMapping("/api/cache/remove")
    public Mono<Long> deleteCache(@Payload CacheContract.KeyRequest input) {
        return cache.delete(input);
    }

    @MessageMapping("/api/message/sendDelayed")
    public Mono<DeliveryReceiptContract.Message> delayed(@Payload DeliveryReceiptContract.SendRequest input) {
        return messages.send(input);
    }

    @MessageMapping("/api/message/findReceipt")
    public Mono<DeliveryReceiptContract.Detail> receipt(@Payload DeliveryReceiptContract.FindRequest input) {
        return messages.receipt(input);
    }

    @MessageMapping("/api/vector/upsert")
    public Mono<Boolean> upsertVector(@Payload VectorContract.UpsertRequest input) {
        return vectors.upsert(input);
    }

    @MessageMapping("/api/vector/search")
    public Flux<VectorContract.Hit> queryVector(@Payload VectorContract.QueryRequest input) {
        return vectors.query(input);
    }
}
