package com.lambda.example.servicea.service;

import com.lambda.example.servicea.contract.SystemContract;
import jakarta.annotation.PostConstruct;
import org.lambda.framework.common.util.ReactiveValues;
import org.lambda.framework.httpclient.HttpclientResponseHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@ConditionalOnProperty("example.partner.base-url")
public class PartnerStatusServiceImpl {
    @Value("${example.partner.base-url}")
    private String baseUrl;
    private WebClient partner;

    @PostConstruct
    void initialize() {
        partner = WebClient.builder().baseUrl(baseUrl)
                .filter(HttpclientResponseHandler.create(body -> !body.isBlank())).build();
    }

    public Mono<SystemContract.PartnerStatus> status(SystemContract.EmptyRequest input) {
        var status = partner.get().uri("/status").retrieve().bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3));
        return ReactiveValues.require(status).map(SystemContract.PartnerStatus::new);
    }
}
