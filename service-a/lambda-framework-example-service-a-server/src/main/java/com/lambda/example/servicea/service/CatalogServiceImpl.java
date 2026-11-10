package com.lambda.example.servicea.service;

import org.lambda.framework.common.util.ReactiveValues;
import com.lambda.example.servicea.enums.ServiceAExceptionEnum;
import org.lambda.framework.common.exception.BusinessException;
import com.lambda.example.servicea.contract.FavoriteContract;
import com.lambda.example.servicea.contract.PurchaseContract;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.lambda.framework.common.support.UiFlux;
import org.lambda.framework.repository.operation.Paging;
import com.lambda.example.servicea.contract.ProductContract;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.time.Duration;
import java.util.Optional;
import org.bson.types.ObjectId;
import com.lambda.example.servicea.po.ProductPO;
import com.lambda.example.servicea.po.PurchasePO;
import com.lambda.example.servicea.repository.FavoriteRepository;
import com.lambda.example.servicea.repository.ProductRepository;
import com.lambda.example.servicea.repository.PurchaseRepository;
import org.lambda.framework.repository.operation.MongoTransactions;
import org.lambda.framework.repository.operation.Paged;

@Service
@Validated
public class CatalogServiceImpl {
    @Resource
    private FavoriteRepository favorites;
    @Resource
    private TransactionalOperator transactions;
    @Resource
    private ProductRepository products;
    @Resource
    private PurchaseRepository purchases;

    public Mono<ProductContract.Detail> create(@Valid @NotNull ProductContract.SaveRequest input) {
        return products.insert(new ProductPO(new ObjectId(), input.name(), input.category(),
                input.price(), Integer.parseInt(input.stock()), input.status())).map(ProductContract.Detail::from);
    }

    public Mono<ProductContract.Detail> find(@Valid @NotNull ProductContract.FindRequest input) {
        return get(input.id()).map(ProductContract.Detail::from);
    }

    public Mono<ProductPO> get(@NotNull ObjectId id) {
        return ReactiveValues.require(products.findById(id), () -> new BusinessException(ServiceAExceptionEnum.PRODUCT_NOT_FOUND));
    }
    public Mono<ProductContract.CursorResult> list(@Valid @NotNull ProductContract.CursorRequest input) {
        int limit = Optional.ofNullable(input.limit()).map(Integer::parseInt).orElse(20);
        var beforeTime = Optional.ofNullable(input.beforeTime());
        var beforeId = Optional.ofNullable(input.beforeId());
        if (limit < 1 || limit > 100 || beforeTime.isPresent() != beforeId.isPresent()) {
            return Mono.error(new BusinessException(ServiceAExceptionEnum.INVALID_CURSOR));
        }
        var cursor = beforeTime.map(time -> new ProductRepository.Cursor(time,
                beforeId.orElseThrow(() -> new BusinessException(ServiceAExceptionEnum.INVALID_CURSOR))));
        return products.findWindow(input.category(), cursor, limit + 1).collectList().map(rows -> {
            boolean more = rows.size() > limit;
            var items = more ? rows.subList(0, limit) : rows;
            var details = items.stream().map(ProductContract.Detail::from).toList();
            if (more) {
                var last = items.getLast();
                return new ProductContract.CursorResult(details, last.getCreatedAt(), last.getId());
            }
            return new ProductContract.CursorResult(details, null, null);
        });
    }
    public UiFlux<ProductContract.Detail> export(@Valid @NotNull ProductContract.CursorRequest input) {
        return UiFlux.of(list(input).flatMapMany(page -> Flux.fromIterable(page.items())));
    }

    public Mono<ProductContract.Detail> update(@Valid @NotNull ProductContract.UpdateRequest request) {
        var input = request.product();
        var changes = new ProductPO(request.id(), input.name(), input.category(),
                input.price(), Integer.parseInt(input.stock()), input.status());
        var updated = products.updateFields(changes, Long.parseLong(request.version()));
        return ReactiveValues.require(updated, () -> new BusinessException(ServiceAExceptionEnum.PRODUCT_CHANGED))
                .map(ProductContract.Detail::from);
    }

    public Mono<Void> favorite(@Valid @NotNull FavoriteContract.ChangeRequest input) {
        return get(input.productId()).then(favorites.saveIfAbsent(input.userId(), input.productId()))
                .onErrorResume(DuplicateKeyException.class, error -> Mono.empty()).then();
    }

    public Mono<Void> unfavorite(@Valid @NotNull FavoriteContract.ChangeRequest input) {
        return favorites.delete(input.userId(), input.productId()).then();
    }

    public Flux<FavoriteContract.Detail> favorites(@Valid @NotNull FavoriteContract.QueryRequest input) {
        return favorites.findRecent(input.userId(), 50).map(FavoriteContract.Detail::from);
    }

    public Mono<PurchaseContract.Detail> purchase(@Valid @NotNull PurchaseContract.CreateRequest input) {
        int quantity = Integer.parseInt(input.quantity());
        return ReactiveValues.optional(purchases.findByRequestId(input.requestId()))
                .flatMap(found -> found.map(existing -> verifyRequest(existing, input, quantity))
                        .orElseGet(() -> createPurchase(input, quantity)))
                .onErrorResume(DuplicateKeyException.class, error ->
                        ReactiveValues.require(purchases.findByRequestId(input.requestId()), () -> error)
                                .flatMap(existing -> verifyRequest(existing, input, quantity)))
                .map(PurchaseContract.Detail::from);
    }

    private Mono<PurchasePO> createPurchase(PurchaseContract.CreateRequest input, int quantity) {
        var updated = products.decrementStock(input.productId(), quantity);
        return ReactiveValues.require(updated, () -> new BusinessException(ServiceAExceptionEnum.STOCK_UNAVAILABLE))
                .flatMap(product -> purchases.insert(new PurchasePO(new ObjectId(), input.requestId(), input.userId(),
                        product.getId(), quantity, product.getPrice())))
                .as(transactions::transactional)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(25)).filter(MongoTransactions::isTransientFailure)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    public Flux<PurchaseContract.CategoryTotal> summarizePurchases(@Valid @NotNull PurchaseContract.SummaryRequest input) {
        return purchases.summarizeByCategory(input.userId())
                .map(total -> new PurchaseContract.CategoryTotal(Optional.ofNullable(total.category()).orElse("未分类"),
                        Long.toString(total.purchases()), Long.toString(total.units())));
    }

    public Mono<Paged<ProductContract.Detail>> page(@Valid @NotNull ProductContract.PageRequest input) {
        int pageNumber = Optional.ofNullable(input.page()).map(Integer::parseInt).orElse(1);
        int size = Optional.ofNullable(input.size()).map(Integer::parseInt).orElse(10);
        if (pageNumber < 1 || size < 1 || size > 100) {
            return Mono.error(new BusinessException(ServiceAExceptionEnum.INVALID_PAGE));
        }
        var paging = Paging.builder().page(pageNumber).size(size).build();
        return products.page(input.category(), paging).map(page -> Paged.<ProductContract.Detail>builder()
                .page(page.getPage()).size(page.getSize()).total(page.getTotal()).pages(page.getPages())
                .records(page.getRecords().stream().map(ProductContract.Detail::from).toList()).build());
    }

    private Mono<PurchasePO> verifyRequest(PurchasePO existing, PurchaseContract.CreateRequest input, int quantity) {
        if (!existing.getUserId().equals(input.userId()) || !existing.getProductId().equals(input.productId()) || existing.getQuantity() != quantity)
            return Mono.error(new BusinessException(ServiceAExceptionEnum.PURCHASE_REQUEST_CONFLICT));
        return Mono.just(existing);
    }
}
