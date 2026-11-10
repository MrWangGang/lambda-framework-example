package com.lambda.example.servicea.service;

import org.lambda.framework.common.util.ReactiveValues;
import com.lambda.example.servicea.enums.ExampleError;
import org.lambda.framework.common.exception.BusinessException;
import com.lambda.example.servicea.facade.ChatFacade;
import jakarta.annotation.Resource;
import org.lambda.framework.common.support.BiFlux;
import org.lambda.framework.compliance.security.PrincipalFactory;
import reactor.core.publisher.Flux;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl {
    @Resource
    private PrincipalFactory principals;

    public BiFlux<ChatFacade.Receipt> exchange(Flux<ChatFacade.Message> messages) {
        return BiFlux.of(ReactiveValues.optional(principals.currentUserId())
                .flatMapMany(principal -> messages.map(message -> receive(message, principal.orElse("anonymous")))));
    }

    private ChatFacade.Receipt receive(ChatFacade.Message message, String principal) {
        if (message.messageId() == null || message.messageId().isBlank()
                || message.text() == null || message.text().isBlank()) {
            throw new BusinessException(ExampleError.INVALID_CHAT_MESSAGE);
        }
        return new ChatFacade.Receipt(message.messageId(), message.text(), principal);
    }
}
