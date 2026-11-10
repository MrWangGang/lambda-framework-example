package com.lambda.example.serviceb.service;

import com.lambda.example.servicea.facade.ChatFacade;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.common.support.BiFlux;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatRelayServiceImpl {
    @RSocketRpc("service-a")
    private ChatFacade chat;

    public BiFlux<ChatFacade.Receipt> exchange(Flux<ChatFacade.Message> messages) {
        return chat.exchange(messages);
    }
}
