package com.lambda.example.servicea.service;

import com.lambda.example.servicea.po.CatalogEventPO;
import jakarta.annotation.PreDestroy;
import org.bson.types.ObjectId;
import org.lambda.framework.common.mq.KafkaMQDeclare;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.guid.GuidFactory;
import org.lambda.framework.mq.config.kafka.KafkaMQReactiveOperate;
import org.lambda.framework.repository.operation.MongoAuditUpdates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

@Service
@ConditionalOnProperty(name = {"lambda.mq.kafka.bootstrap-servers", "example.kafka.topic"})
public class CatalogEventService {
    private static final Logger log = LoggerFactory.getLogger(CatalogEventService.class);
    private final KafkaMQReactiveOperate kafka;
    private final KafkaMQDeclare events;
    private final CatalogService catalog;
    private final ReactiveMongoTemplate mongo;
    private final GuidFactory ids;
    private Disposable consumer;

    public CatalogEventService(KafkaMQReactiveOperate kafka, CatalogService catalog, ReactiveMongoTemplate mongo,
                               GuidFactory ids, @Value("${example.kafka.topic}") String topic,
                               @Value("${example.kafka.group:lambda_catalog}") String group) {
        this.kafka = kafka;
        this.catalog = catalog;
        this.mongo = mongo;
        this.ids = ids;
        this.events = new KafkaMQDeclare(topic, group);
    }

    public Mono<Event> publish(ObjectId productId) {
        return catalog.get(productId).flatMap(product -> {
            var event = new Event(ids.nextId(), product.getId(), Instant.now());
            return kafka.send(events, productId.toHexString(), JsonUtil.objToString(event)).thenReturn(event);
        });
    }

    public Mono<Event> find(String eventId) {
        return mongo.findOne(eventQuery(eventId), CatalogEventPO.class)
                .map(event -> new Event(event.getEventId(), event.getProductId(), event.getCreatedAt()));
    }

    @EventListener(ApplicationReadyEvent.class)
    void consumeCatalogEvents() {
        consumer = kafka.receiverManual(events)
                .concatMap(record -> Mono.defer(() -> {
                    var event = JsonUtil.stringToObj(record.value(), Event.class).orElseThrow();
                    var update = new Update().setOnInsert("eventId", event.eventId())
                            .setOnInsert("productId", event.productId());
                    return mongo.upsert(eventQuery(event.eventId()),
                                    MongoAuditUpdates.insertOnly(update, event.createdAt(), Instant.now()),
                                    CatalogEventPO.class)
                            .then(record.receiverOffset().commit());
                }))
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(15)))
                .subscribe(ignored -> {}, error -> log.error("Catalog event consumption stopped", error));
    }

    private static Query eventQuery(String eventId) {
        return Query.query(Criteria.where("eventId").is(eventId));
    }

    @PreDestroy
    void close() {
        if (consumer != null) consumer.dispose();
    }

    public record Event(String eventId, ObjectId productId, Instant createdAt) {}
}
