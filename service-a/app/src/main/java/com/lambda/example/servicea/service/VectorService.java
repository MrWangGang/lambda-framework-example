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

    public Mono<Boolean> upsert(UUID pointId, List<Float> input, String category) {
        return Mono.defer(() -> vectors.save(pointId, validate(input), category));
    }

    public Flux<VectorHit> query(List<Float> input, int limit) {
        return Flux.defer(() -> vectors.search(validate(input), limit))
                .map(match -> new VectorHit(match.id(), match.score(), match.category()));
    }

    private static List<Float> validate(List<Float> input) {
        if (input.size() != VectorRepository.DIMENSIONS
                || input.stream().anyMatch(value -> value == null || !Float.isFinite(value))
                || input.stream().allMatch(value -> value == 0.0f)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use three finite values and a nonzero vector");
        }
        return input;
    }

    public record VectorHit(String id, float score, String category) {}
}
