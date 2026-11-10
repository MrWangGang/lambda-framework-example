package com.lambda.example.servicea.service;

import com.lambda.example.servicea.repository.VectorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Service
public class VectorService {
    private final VectorRepository vectors;

    public VectorService(VectorRepository vectors) {
        this.vectors = vectors;
    }

    public Mono<Boolean> upsert(UUID pointId, List<Double> input, String category) {
        return Mono.defer(() -> vectors.save(pointId, validate(input), category));
    }

    public Flux<VectorHit> query(List<Double> input, int limit) {
        return Flux.defer(() -> vectors.search(validate(input), limit))
                .map(match -> new VectorHit(match.id(), match.score(), match.category()));
    }

    private static List<Float> validate(List<Double> input) {
        var values = input.stream().map(Double::floatValue).toList();
        if (values.size() != VectorRepository.DIMENSIONS || values.stream().anyMatch(value -> !Float.isFinite(value))
                || values.stream().allMatch(value -> value == 0.0f)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use three finite values and a nonzero vector");
        }
        return values;
    }

    public record VectorHit(String id, float score, String category) {}
}
