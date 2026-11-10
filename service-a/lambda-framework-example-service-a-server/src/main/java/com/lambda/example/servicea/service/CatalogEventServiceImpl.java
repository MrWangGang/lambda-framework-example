package com.lambda.example.servicea.service;

import org.lambda.framework.common.util.ReactiveValues;
import com.lambda.example.servicea.enums.ServiceAExceptionEnum;
import org.lambda.framework.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.lambda.example.servicea.contract.CatalogEventContract.Event;
import com.lambda.example.servicea.contract.CatalogEventContract.FindRequest;
import com.lambda.example.servicea.contract.CatalogEventContract.PublishRequest;
import com.lambda.example.servicea.repository.CatalogEventRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.lambda.framework.common.mq.KafkaMQDeclare;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.guid.GuidFactory;
import org.lambda.framework.mq.config.kafka.KafkaMQReactiveOperate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;

@Service
@Validated
@ConditionalOnProperty(name = {"lambda.mq.kafka.bootstrap-servers", "example.kafka.topic"})
public class CatalogEventServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(CatalogEventServiceImpl.class);
    @Resource
    private KafkaMQReactiveOperate kafka;
    @Resource
    private CatalogServiceImpl catalog;
    @Resource
    private CatalogEventRepository eventRepository;
    @Resource
    private GuidFactory ids;
    @Value("${example.kafka.topic}")
    private String topic;
    @Value("${example.kafka.group:lambda_catalog}")
    private String group;
    private KafkaMQDeclare events;
    private Disposable consumer;

    @PostConstruct
    void initialize() {
        events = new KafkaMQDeclare(topic, group);
    }

    public Mono<Event> publish(@Valid @NotNull PublishRequest request) {
        var productId = request.productId();
        return catalog.get(productId).flatMap(product -> {
            var event = new Event(ids.nextId(), product.getId(), Instant.now());
            return kafka.send(events, productId.toHexString(), JsonUtil.write(event)).thenReturn(event);
        });
    }

    public Mono<Event> find(@Valid @NotNull FindRequest request) {
        return ReactiveValues.require(eventRepository.findByEventId(request.eventId()),
                        () -> new BusinessException(ServiceAExceptionEnum.EVENT_NOT_FOUND))
                .map(event -> new Event(event.getEventId(), event.getProductId(), event.getCreatedAt()));
    }

    @EventListener(ApplicationReadyEvent.class)
    void consumeCatalogEvents() {
        consumer = kafka.receiverManual(events)
                .concatMap(record -> Mono.defer(() -> {
                    var event = JsonUtil.readRequired(record.value(), Event.class);
                    return eventRepository.saveIfAbsent(event.eventId(), event.productId(), event.createdAt())
                            .then(record.receiverOffset().commit());
                }))
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(15)))
                .subscribe(ignored -> {}, error -> log.error("Catalog event consumption stopped", error));
    }

    @PreDestroy
    void close() {
        if (consumer != null) consumer.dispose();
    }
}
