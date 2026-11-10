package com.lambda.example.servicea.service;

import com.lambda.example.servicea.enums.ExampleError;
import org.lambda.framework.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.lambda.example.servicea.contract.VectorContract;
import com.lambda.example.servicea.repository.VectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Validated
public class VectorServiceImpl {
    @Resource
    private VectorRepository vectors;

    public Mono<Boolean> upsert(@Valid @NotNull VectorContract.UpsertRequest input) {
        return Mono.defer(() -> vectors.save(input.id(), validate(input.vector()), input.category()));
    }

    public Flux<VectorContract.Hit> query(@Valid @NotNull VectorContract.QueryRequest input) {
        return Flux.defer(() -> vectors.search(validate(input.vector()), Integer.parseInt(input.limit())))
                .map(match -> new VectorContract.Hit(match.id(), match.score(), match.category()));
    }

    private static List<Float> validate(List<Float> input) {
        if (input.size() != VectorRepository.DIMENSIONS
                || input.stream().anyMatch(value -> value == null || !Float.isFinite(value))
                || input.stream().allMatch(value -> value == 0.0f)) {
            throw new BusinessException(ExampleError.INVALID_VECTOR);
        }
        return input;
    }
}
