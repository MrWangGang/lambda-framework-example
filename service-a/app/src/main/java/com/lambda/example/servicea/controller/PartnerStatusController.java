package com.lambda.example.servicea.controller;

import org.lambda.framework.httpclient.HttpclientResponseHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Controller
@ConditionalOnProperty("example.partner.base-url")
public class PartnerStatusController {
    private final WebClient partner;

    public PartnerStatusController(@Value("${example.partner.base-url}") String baseUrl) {
        this.partner = WebClient.builder().baseUrl(baseUrl)
                .filter(HttpclientResponseHandler.create(body -> !body.isBlank())).build();
    }

    @MessageMapping("/api/partner/status")
    public Mono<Status> status(@Payload EmptyRequest input) {
        return partner.get().uri("/status").retrieve().bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3)).map(Status::new);
    }

    public record EmptyRequest() {}
    public record Status(String partnerStatus) {}
}
