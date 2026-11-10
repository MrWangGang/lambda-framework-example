package com.lambda.example.servicea.facade;

import com.lambda.example.servicea.service.ChatServiceImpl;
import jakarta.annotation.Resource;
import org.lambda.framework.common.annotation.rsocket.RSocketFacade;
import org.lambda.framework.common.support.BiFlux;
import reactor.core.publisher.Flux;

@RSocketFacade
public class ChatFacadeImpl implements ChatFacade {
    @Resource
    private ChatServiceImpl chat;

    @Override
    public BiFlux<Receipt> exchange(Flux<Message> messages) {
        return chat.exchange(messages);
    }
}
