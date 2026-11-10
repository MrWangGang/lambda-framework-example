package com.lambda.example.servicea.service;

import org.lambda.framework.common.util.ReactiveValues;
import com.lambda.example.servicea.enums.ServiceAExceptionEnum;
import org.lambda.framework.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.lambda.example.servicea.contract.CacheContract;
import org.lambda.framework.repository.operation.redis.ReactiveRedisOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Service
@Validated
public class CacheServiceImpl {
    @Resource
    private ReactiveRedisOperation redis;
    @Value("${example.resource-prefix}:cache:")
    private String prefix;

    public Mono<Boolean> put(@Valid @NotNull CacheContract.PutRequest input) {
        return put(input.key(), input.value(), Long.parseLong(input.ttlSeconds()));
    }

    public Mono<String> get(@Valid @NotNull CacheContract.KeyRequest input) {
        return ReactiveValues.require(redis.<String, String>get(key(input.key())),
                () -> new BusinessException(ServiceAExceptionEnum.CACHE_NOT_FOUND));
    }

    public Mono<Long> delete(@Valid @NotNull CacheContract.KeyRequest input) {
        return redis.delete(key(input.key()));
    }

    private String key(String key) {
        if (!key.matches("[A-Za-z0-9_-]{1,80}")) throw new BusinessException(ServiceAExceptionEnum.INVALID_CACHE_KEY);
        return prefix + key;
    }
    public Mono<Boolean> put(String key, String value, long ttlSeconds) {
        if (ttlSeconds < 1 || ttlSeconds > 86400) return Mono.error(new BusinessException(ServiceAExceptionEnum.INVALID_CACHE_TTL));
        return redis.set(key(key), value, ttlSeconds);
    }
}
