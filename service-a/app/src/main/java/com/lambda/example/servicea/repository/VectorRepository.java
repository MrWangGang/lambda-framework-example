package com.lambda.example.servicea.repository;

import io.qdrant.client.grpc.Collections.CreateCollection;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.UpsertPoints;
import org.lambda.framework.repository.operation.qdrant.ReactiveQdrantOperations;
import org.lambda.framework.repository.operation.qdrant.ReactiveQdrantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.QueryFactory.nearest;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;

@Repository
public class VectorRepository extends ReactiveQdrantRepository<VectorRepository.Match> {
    public static final int DIMENSIONS = 3;
    private final int replicas;

    public VectorRepository(ReactiveQdrantOperations qdrant, @Value("${example.resource-prefix}") String prefix,
                            @Value("${example.vector.replication-factor:1}") int replicas) {
        super(qdrant, prefix + "_vectors", VectorRepository::match);
        this.replicas = replicas;
    }

    @Bean
    ApplicationRunner vectorCollection() {
        return args -> ensureCollection(CreateCollection.newBuilder().setReplicationFactor(replicas)
                .setVectorsConfig(VectorsConfig.newBuilder()
                        .setParams(VectorParams.newBuilder().setSize(DIMENSIONS).setDistance(Distance.Cosine))))
                .block(Duration.ofSeconds(8));
    }

    public Mono<Boolean> save(UUID pointId, List<Float> embedding, String category) {
        var point = PointStruct.newBuilder().setId(id(pointId)).setVectors(vectors(embedding))
                .putPayload("category", value(category)).build();
        return upsert(UpsertPoints.newBuilder().setWait(true).addPoints(point)).thenReturn(true);
    }

    public Flux<Match> search(List<Float> embedding, int limit) {
        return query(QueryPoints.newBuilder().setQuery(nearest(embedding)).setLimit(limit)
                .setWithPayload(enable(true)));
    }

    private static Match match(ScoredPoint point) {
        return new Match(point.getId().getUuid(), point.getScore(),
                point.getPayloadOrDefault("category", value("")).getStringValue());
    }

    public record Match(String id, float score, String category) {}
}
