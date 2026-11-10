package com.lambda.example.servicea.service;

import com.lambda.example.servicea.repository.ProductRepository;
import org.lambda.framework.guid.GuidFactory;
import org.lambda.framework.lock.ReactiveRedissonBuilder;
import org.lambda.framework.lock.ReactiveZookeeperBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

@Service
public class CatalogMaintenanceService {
    private static final Duration WAIT = Duration.ofSeconds(2);
    private static final Duration LEASE = Duration.ofSeconds(10);
    private final ProductRepository products;
    private final CacheService cache;
    private final GuidFactory ids;
    private final ReactiveRedissonBuilder redisLocks;
    private final ObjectProvider<ReactiveZookeeperBuilder> zookeeperLocks;
    private final String prefix;

    public CatalogMaintenanceService(ProductRepository products, CacheService cache, GuidFactory ids,
                                     ReactiveRedissonBuilder redisLocks, ObjectProvider<ReactiveZookeeperBuilder> zookeeperLocks,
                                     @Value("${example.resource-prefix}") String prefix) {
        this.products = products;
        this.cache = cache;
        this.ids = ids;
        this.redisLocks = redisLocks;
        this.zookeeperLocks = zookeeperLocks;
        this.prefix = prefix;
    }

    public Mono<RebuildResult> rebuild(String category, LockProvider provider) {
        String lock = prefix + "-catalog-count-" + category;
        Supplier<Mono<RebuildResult>> action = () -> products.countCategory(category).flatMap(count ->
                cache.put("catalog-count-" + category, Long.toString(count), 300)
                        .thenReturn(new RebuildResult(ids.nextId(), Long.toString(count))));
        if (provider == LockProvider.REDIS) {
            return redisLocks.getLock(lock).withLock(WAIT, LEASE, action);
        }
        var zookeeper = zookeeperLocks.getIfAvailable();
        return zookeeper == null
                ? Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ZooKeeper is disabled"))
                : zookeeper.getLock(lock).withLock(WAIT, action);
    }

    public enum LockProvider { REDIS, ZOOKEEPER }

    public record RebuildResult(String jobId, String productCount) {}
}
