package com.lambda.example.servicea.controller;

import com.lambda.example.servicea.contract.CatalogEventContract;
import com.lambda.example.servicea.service.CatalogEventServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@ConditionalOnProperty(name = {"lambda.mq.kafka.bootstrap-servers", "example.kafka.topic"})
public class CatalogEventController {
    @Resource
    private CatalogEventServiceImpl events;

    @MessageMapping("/api/catalog/event/publish")
    public Mono<CatalogEventContract.Event> publish(@Payload CatalogEventContract.PublishRequest input) {
        return events.publish(input);
    }

    @MessageMapping("/api/catalog/event/find")
    public Mono<CatalogEventContract.Event> find(@Payload CatalogEventContract.FindRequest input) {
        return events.find(input);
    }
}
