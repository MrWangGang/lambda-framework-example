package com.lambda.example.servicea.facade;

import com.lambda.example.servicea.service.ChatServiceImpl;
import org.lambda.framework.common.annotation.rsocket.RSocketFacade;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.compliance.security.PrincipalFactory;
import reactor.core.publisher.Flux;

@RSocketFacade
public class ChatFacadeImpl implements ChatFacade {
    private final ChatServiceImpl chat;
    private final PrincipalFactory principals;

    public ChatFacadeImpl(ChatServiceImpl chat, PrincipalFactory principals) {
        this.chat = chat;
        this.principals = principals;
    }

    @Override
    public BiFlux<Receipt> exchange(Flux<Message> messages) {
        return BiFlux.of(principals.currentUserId().defaultIfEmpty("anonymous")
                .flatMapMany(principal -> messages.map(message -> chat.receive(message, principal))));
    }
}
