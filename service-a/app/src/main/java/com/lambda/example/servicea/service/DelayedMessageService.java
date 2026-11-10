package com.lambda.example.servicea.service;

import jakarta.annotation.PreDestroy;
import org.bson.types.ObjectId;
import com.lambda.example.servicea.entity.DeliveryReceiptEntity;
import com.lambda.example.servicea.entity.DeliveryReceiptEntity.MessageDTO;
import org.lambda.framework.common.mq.RabbitMQDeclare;
import org.lambda.framework.common.util.sample.JsonUtil;
import org.lambda.framework.mq.config.rabbitmq.RabbitMQReactiveOperate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.*;
import reactor.util.retry.Retry;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class DelayedMessageService {
    private static final Logger log = LoggerFactory.getLogger(DelayedMessageService.class);
    private final Sender sender;
    private final Receiver receiver;
    private final ReactiveMongoTemplate mongo;
    private final RabbitMQReactiveOperate rabbit;
    private final String exchange;
    private final String queue;
    private final Map<String, Object> queueType;
    private Disposable consumer;
    public DelayedMessageService(Sender sender, Receiver receiver, ReactiveMongoTemplate mongo, RabbitMQReactiveOperate rabbit,
                                 @Value("${example.resource-prefix}") String prefix,
                                 @Value("${lambda.mq.rabbitmq.mode:single}") String mode) {
        this.sender = sender;
        this.receiver = receiver;
        this.mongo = mongo;
        this.rabbit = rabbit;
        this.exchange = prefix + ".delayed";
        this.queue = prefix + ".notifications";
        this.queueType = "cluster".equalsIgnoreCase(mode) ? Map.of("x-queue-type", "quorum") : Map.of();
    }
    @Bean
    ApplicationRunner messageTopology() {
        return args -> {
            var arguments = new HashMap<>(queueType);
            arguments.put("x-dead-letter-exchange", exchange + ".dead");
            arguments.put("x-dead-letter-routing-key", "dead");
            sender.declareExchange(ExchangeSpecification.exchange(exchange + ".dead").type("direct").durable(true))
                    .then(sender.declareQueue(QueueSpecification.queue(queue + ".dead").durable(true).arguments(queueType)))
                    .then(sender.bind(BindingSpecification.binding(exchange + ".dead", "dead", queue + ".dead")))
                    .then(rabbit.declareDelay(declaration(), arguments))
                    .block(Duration.ofSeconds(10));
            consumer = receiver.consumeManualAck(queue, new ConsumeOptions().qos(32))
                    .flatMap(delivery -> Mono.defer(() -> {
                        MessageDTO message = JsonUtil.stringToObj(new String(delivery.getBody(), StandardCharsets.UTF_8), MessageDTO.class)
                                .orElseThrow(() -> new IllegalArgumentException("Invalid message"));
                        Query id = Query.query(Criteria.where("_id").is(message.id()));
                        Update receipt = new Update().setOnInsert("content", message.content())
                                .setOnInsert("createdAt", message.createdAt()).setOnInsert("receivedAt", Instant.now());
                        return mongo.upsert(id, receipt, DeliveryReceiptEntity.class).doOnSuccess(result -> delivery.ack());
                    }).onErrorResume(error -> {
                        log.error("Delivery failed; routing to dead-letter queue", error);
                        delivery.nack(false);
                        return Mono.empty();
                    }), 8)
                    .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(30)))
                    .subscribe(result -> {}, error -> log.error("Notification consumer stopped", error));
        };
    }
    public Mono<MessageDTO> send(DeliveryReceiptEntity.SendDTO input) {
        long delayMillis = Long.parseLong(input.delayMillis());
        if (delayMillis > 60000) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "delayMillis must be 0..60000"));
        }
        MessageDTO message = new MessageDTO(new ObjectId(), input.content(), Instant.now());
        return rabbit.sendDelay(declaration(), delayMillis, JsonUtil.objToString(message)).thenReturn(message);
    }

    private RabbitMQDeclare declaration() {
        return new RabbitMQDeclare("notify", exchange, queue);
    }

    public Mono<DeliveryReceiptEntity> receipt(ObjectId id) {
        return mongo.findById(id, DeliveryReceiptEntity.class)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not received yet")));
    }
    @PreDestroy
    void close() { if (consumer != null) consumer.dispose(); }
}
