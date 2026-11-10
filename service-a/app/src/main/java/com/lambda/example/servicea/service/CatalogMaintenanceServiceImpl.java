package com.lambda.example.servicea.service;

import com.lambda.example.servicea.enums.ExampleError;
import org.lambda.framework.common.exception.BusinessException;
import org.lambda.framework.common.util.OptionalUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.lambda.example.servicea.contract.CatalogMaintenanceContract.LockProvider;
import com.lambda.example.servicea.contract.CatalogMaintenanceContract.RebuildRequest;
import com.lambda.example.servicea.contract.CatalogMaintenanceContract.RebuildResult;
import com.lambda.example.servicea.repository.ProductRepository;
import org.lambda.framework.guid.GuidFactory;
import org.lambda.framework.lock.ReactiveRedissonBuilder;
import org.lambda.framework.lock.ReactiveZookeeperBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@Validated
public class CatalogMaintenanceServiceImpl {
    private static final Duration WAIT = Duration.ofSeconds(2);
    private static final Duration LEASE = Duration.ofSeconds(10);
    @Resource
    private ProductRepository products;
    @Resource
    private CacheServiceImpl cache;
    @Resource
    private GuidFactory ids;
    @Resource
    private ReactiveRedissonBuilder redisLocks;
    @Autowired
    private ObjectProvider<ReactiveZookeeperBuilder> zookeeperLocks;
    @Value("${example.resource-prefix}")
    private String prefix;

    public Mono<RebuildResult> rebuild(@Valid @NotNull RebuildRequest request) {
        String category = request.category();
        String lock = prefix + "-catalog-count-" + category;
        Supplier<Mono<RebuildResult>> action = () -> products.countCategory(category).flatMap(count ->
                cache.put("catalog-count-" + category, Long.toString(count), 300)
                        .thenReturn(new RebuildResult(ids.nextId(), Long.toString(count))));
        if (request.lockProvider() == LockProvider.REDIS) {
            return redisLocks.getLock(lock).withLock(WAIT, LEASE, action);
        }
        var zookeeper = OptionalUtil.require(zookeeperLocks.getIfAvailable(),
                () -> new BusinessException(ExampleError.ZOOKEEPER_DISABLED));
        return zookeeper.getLock(lock).withLock(WAIT, action);
    }
}
