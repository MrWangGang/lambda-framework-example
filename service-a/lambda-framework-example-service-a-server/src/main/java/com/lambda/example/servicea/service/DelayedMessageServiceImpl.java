package com.lambda.example.servicea.service;

import org.lambda.framework.common.util.ReactiveValues;
import com.lambda.example.servicea.enums.ServiceAExceptionEnum;
import org.lambda.framework.common.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.lambda.example.servicea.contract.DeliveryReceiptContract;
import com.lambda.example.servicea.repository.DeliveryReceiptRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.types.ObjectId;
import com.lambda.example.servicea.contract.DeliveryReceiptContract.Message;
import org.lambda.framework.common.mq.RabbitMQDeclare;
import org.lambda.framework.common.util.JsonUtil;
import org.lambda.framework.mq.config.rabbitmq.RabbitMQReactiveOperate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class DelayedMessageServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(DelayedMessageServiceImpl.class);
    @Resource
    private Sender sender;
    @Resource
    private Receiver receiver;
    @Resource
    private DeliveryReceiptRepository receipts;
    @Resource
    private RabbitMQReactiveOperate rabbit;
    @Value("${example.resource-prefix}")
    private String prefix;
    @Value("${lambda.mq.rabbitmq.mode:single}")
    private String mode;
    private String exchange;
    private String queue;
    private Map<String, Object> queueType;
    private Disposable consumer;

    @PostConstruct
    void initialize() {
        exchange = prefix + ".delayed";
        queue = prefix + ".notifications";
        queueType = "cluster".equalsIgnoreCase(mode) ? Map.of("x-queue-type", "quorum") : Map.of();
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
                        Message message = JsonUtil.readRequired(new String(delivery.getBody(), StandardCharsets.UTF_8), Message.class,
                                () -> new BusinessException(ServiceAExceptionEnum.INVALID_NOTIFICATION));
                        if (message.id() == null || message.content() == null || message.content().isBlank()
                                || message.content().length() > 10000 || message.createdAt() == null) {
                            return Mono.error(new BusinessException(ServiceAExceptionEnum.INVALID_NOTIFICATION));
                        }
                        return receipts.saveIfAbsent(message.id(), message.content(), message.createdAt(), Instant.now())
                                .doOnSuccess(result -> delivery.ack());
                    }).onErrorResume(error -> {
                        log.error("Delivery failed; routing to dead-letter queue", error);
                        delivery.nack(false);
                        return Mono.empty();
                    }), 8)
                    .retryWhen(Retry.backoff(10, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(30)))
                    .subscribe(result -> {}, error -> log.error("Notification consumer stopped", error));
        };
    }
    public Mono<Message> send(@Valid @NotNull DeliveryReceiptContract.SendRequest input) {
        long delayMillis = Long.parseLong(input.delayMillis());
        if (delayMillis > 60000) {
            return Mono.error(new BusinessException(ServiceAExceptionEnum.INVALID_DELAY));
        }
        Message message = new Message(new ObjectId(), input.content(), Instant.now());
        return rabbit.sendDelay(declaration(), delayMillis, JsonUtil.write(message)).thenReturn(message);
    }

    private RabbitMQDeclare declaration() {
        return new RabbitMQDeclare("notify", exchange, queue);
    }

    public Mono<DeliveryReceiptContract.Detail> receipt(@Valid @NotNull DeliveryReceiptContract.FindRequest request) {
        return ReactiveValues.require(receipts.findById(request.id()),
                        () -> new BusinessException(ServiceAExceptionEnum.RECEIPT_NOT_FOUND))
                .map(DeliveryReceiptContract.Detail::from);
    }
    @PreDestroy
    void close() { if (consumer != null) consumer.dispose(); }
}
