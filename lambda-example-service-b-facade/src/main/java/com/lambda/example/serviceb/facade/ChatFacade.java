package com.lambda.example.serviceb.facade;

import org.lambda.framework.common.support.BiFlux;
import org.springframework.messaging.handler.annotation.MessageMapping;
import reactor.core.publisher.Flux;

public interface ChatFacade {
    @MessageMapping("lambda-example-service-b.chatFacade.exchange")
    BiFlux<Receipt> exchange(Flux<Message> messages);

    record Message(String messageId, String text) {}

    record Receipt(String messageId, String text) {}
}
