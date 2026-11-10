package com.lambda.example.serviceb.controller;

import com.lambda.example.servicea.facade.ChatFacade;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.common.support.BiFlux;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class ChatController {
    @RSocketRpc("service-a")
    private ChatFacade chat;

    @MessageMapping("/api/chat/exchange")
    public BiFlux<ChatFacade.Receipt> exchange(Flux<ChatFacade.Message> messages) {
        return chat.exchange(messages);
    }
}
