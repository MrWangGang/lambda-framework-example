package com.lambda.example.servicea.controller;

import com.lambda.example.servicea.service.CatalogEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.bson.types.ObjectId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@ConditionalOnProperty(name = {"lambda.mq.kafka.bootstrap-servers", "example.kafka.topic"})
public class CatalogEventController {
    private final CatalogEventService events;

    public CatalogEventController(CatalogEventService events) {
        this.events = events;
    }

    @MessageMapping("/api/catalog/event/publish")
    public Mono<CatalogEventService.Event> publish(@Valid @Payload PublishRequest input) {
        return events.publish(input.productId());
    }

    @MessageMapping("/api/catalog/event/find")
    public Mono<CatalogEventService.Event> find(@Valid @Payload FindRequest input) {
        return events.find(input.eventId());
    }

    public record PublishRequest(@NotNull ObjectId productId) {}
    public record FindRequest(@NotNull @Pattern(regexp = "[0-9]{1,19}") String eventId) {}
}
