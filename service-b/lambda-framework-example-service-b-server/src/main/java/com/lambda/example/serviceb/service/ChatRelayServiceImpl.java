package com.lambda.example.serviceb.service;

import com.lambda.example.servicea.facade.ChatFacade;
import com.lambda.example.serviceb.enums.ServiceBExceptionEnum;
import org.lambda.framework.common.annotation.rsocket.RSocketRpc;
import org.lambda.framework.common.exception.BusinessException;
import org.lambda.framework.common.support.BiFlux;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatRelayServiceImpl {
    @RSocketRpc("service-a")
    private ChatFacade chat;

    public BiFlux<ChatFacade.Receipt> exchange(Flux<ChatFacade.Message> messages) {
        return chat.exchange(messages.switchIfEmpty(
                Flux.error(() -> new BusinessException(ServiceBExceptionEnum.CHAT_MESSAGE_REQUIRED))));
    }
}
