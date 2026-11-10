package com.lambda.example.serviceb.facade;

import com.lambda.example.serviceb.service.ChatServiceImpl;
import org.lambda.framework.common.support.BiFlux;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class ChatFacadeImpl implements ChatFacade {
    private final ChatServiceImpl chat;

    public ChatFacadeImpl(ChatServiceImpl chat) {
        this.chat = chat;
    }

    @Override
    public BiFlux<Receipt> exchange(Flux<Message> messages) {
        return BiFlux.of(messages.map(chat::receive));
    }
}
