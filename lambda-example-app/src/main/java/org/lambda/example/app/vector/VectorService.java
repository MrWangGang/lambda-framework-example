package org.lambda.example.app.vector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VectorService {
    private final WebClient client;
    private final String collection;
    public VectorService(WebClient.Builder builder, @Value("${example.qdrant.url}") String url,
                         @Value("${example.qdrant.api-key}") String apiKey,
                         @Value("${example.resource-prefix}") String prefix) {
        client = builder.baseUrl(url).defaultHeader("api-key", apiKey).build();
        collection = prefix + "_vectors";
    }
    @Bean
    ApplicationRunner vectorCollection() {
        return args -> client.get().uri("/collections/{name}", collection).retrieve().bodyToMono(String.class)
                .onErrorResume(WebClientResponseException.NotFound.class, error -> client.put().uri("/collections/{name}", collection)
                        .bodyValue(Map.of("vectors", Map.of("size", 3, "distance", "Cosine")))
                        .retrieve().bodyToMono(String.class)).block(Duration.ofSeconds(30));
    }
    public Mono<String> upsert(String id, List<Double> vector, String category) {
        UUID.fromString(id);
        validateVector(vector);
        return client.put().uri("/collections/{name}/points?wait=true", collection)
                .bodyValue(Map.of("points", List.of(Map.of("id", id, "vector", vector, "payload", Map.of("category", category)))))
                .retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(10));
    }
    public Mono<String> query(List<Double> vector, int limit) {
        validateVector(vector);
        return client.post().uri("/collections/{name}/points/query", collection)
                .bodyValue(Map.of("query", vector, "limit", limit, "with_payload", true))
                .retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(10));
    }
    private static void validateVector(List<Double> vector) {
        if (vector.size() != 3 || vector.stream().anyMatch(value -> value == null || !Double.isFinite(value))
                || vector.stream().allMatch(value -> value == 0.0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use three finite values and a nonzero vector");
    }
}
