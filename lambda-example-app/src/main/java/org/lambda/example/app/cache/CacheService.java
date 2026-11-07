package org.lambda.example.app.cache;

import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class CacheService {
    private final ReactiveRedisOperation redis;
    private final String prefix;
    public CacheService(ReactiveRedisOperation redis, @Value("${example.resource-prefix}") String prefix) {
        this.redis = redis;
        this.prefix = prefix + ":cache:";
    }
    private String key(String key) {
        if (!key.matches("[A-Za-z0-9_-]{1,80}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cache key");
        return prefix + key;
    }
    public Mono<Boolean> put(String key, String value, long ttlSeconds) {
        if (ttlSeconds < 1 || ttlSeconds > 86400) return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ttlSeconds must be 1..86400"));
        return redis.set(key(key), value, ttlSeconds);
    }
    public Mono<String> get(String key) {
        return redis.<String,String>get(key(key)).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cache entry not found")));
    }
    public Mono<Long> delete(String key) { return redis.delete(key(key)); }
}
