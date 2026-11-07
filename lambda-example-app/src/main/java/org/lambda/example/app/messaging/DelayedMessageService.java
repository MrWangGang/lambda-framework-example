package org.lambda.example.app.messaging;

import com.rabbitmq.client.AMQP;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.lambda.framework.common.util.sample.JsonUtil;
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
import java.util.Map;
import java.util.UUID;

@Service
public class DelayedMessageService {
    private static final Logger log = LoggerFactory.getLogger(DelayedMessageService.class);
    public record Message(String id, String content, Instant createdAt) {
        public Message {
            UUID.fromString(id);
            if (content == null || content.isBlank() || content.length() > 10000 || createdAt == null)
                throw new IllegalArgumentException("Invalid notification fields");
        }
    }
    private final Sender sender;
    private final Receiver receiver;
    private final ReactiveMongoTemplate mongo;
    private final String exchange;
    private final String queue;
    private Disposable consumer;
    public DelayedMessageService(Sender sender, Receiver receiver, ReactiveMongoTemplate mongo,
                                 @Value("${example.resource-prefix}") String prefix) {
        this.sender = sender; this.receiver = receiver; this.mongo = mongo;
        this.exchange = prefix + ".delayed"; this.queue = prefix + ".notifications";
    }
    @Bean
    ApplicationRunner messageTopology() {
        return args -> {
            sender.declareExchange(ExchangeSpecification.exchange(exchange).type("x-delayed-message").durable(true)
                            .arguments(Map.of("x-delayed-type", "direct")))
                    .then(sender.declareExchange(ExchangeSpecification.exchange(exchange + ".dead").type("direct").durable(true)))
                    .then(sender.declareQueue(QueueSpecification.queue(queue + ".dead").durable(true)))
                    .then(sender.bind(BindingSpecification.binding(exchange + ".dead", "dead", queue + ".dead")))
                    .then(sender.declareQueue(QueueSpecification.queue(queue).durable(true)
                            .arguments(Map.of("x-dead-letter-exchange", exchange + ".dead", "x-dead-letter-routing-key", "dead"))))
                    .then(sender.bind(BindingSpecification.binding(exchange, "notify", queue)))
                    .block(Duration.ofSeconds(30));
            consumer = receiver.consumeManualAck(queue, new ConsumeOptions().qos(32))
                    .flatMap(delivery -> Mono.defer(() -> {
                        Message message = JsonUtil.stringToObj(new String(delivery.getBody(), StandardCharsets.UTF_8), Message.class)
                                .orElseThrow(() -> new IllegalArgumentException("Invalid message"));
                        Query id = Query.query(Criteria.where("_id").is(message.id()));
                        Update receipt = new Update().setOnInsert("content", message.content())
                                .setOnInsert("createdAt", message.createdAt()).setOnInsert("receivedAt", Instant.now());
                        return mongo.upsert(id, receipt, "delivery_receipts").doOnSuccess(result -> delivery.ack());
                    }).onErrorResume(error -> {
                        log.error("Delivery failed; routing to dead-letter queue", error);
                        delivery.nack(false);
                        return Mono.empty();
                    }), 8)
                    .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(30)))
                    .subscribe(result -> {}, error -> log.error("Notification consumer stopped", error));
        };
    }
    public Mono<Message> send(String content, long delayMillis) {
        Message message = new Message(UUID.randomUUID().toString(), content, Instant.now());
        var properties = new AMQP.BasicProperties.Builder().deliveryMode(2).contentType("application/json")
                .headers(Map.of("x-delay", delayMillis)).build();
        var outbound = new OutboundMessage(exchange, "notify", properties, JsonUtil.objToString(message).getBytes(StandardCharsets.UTF_8));
        return sender.sendWithPublishConfirms(Mono.just(outbound)).single().flatMap(result -> result.isAck()
                ? Mono.just(message) : Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Broker did not confirm message")));
    }
    public Mono<Document> receipt(String id) {
        return mongo.findById(id, Document.class, "delivery_receipts")
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not received yet")));
    }
    @PreDestroy
    void close() { if (consumer != null) consumer.dispose(); }
}
