package com.lambda.example.servicea.service;

import com.lambda.example.servicea.facade.ChatFacade;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl {
    public ChatFacade.Receipt receive(ChatFacade.Message message, String principal) {
        if (message.messageId() == null || message.messageId().isBlank()
                || message.text() == null || message.text().isBlank()) {
            throw new GlobalException("EXAMPLE_CHAT_001", "消息编号和正文不能为空");
        }
        return new ChatFacade.Receipt(message.messageId(), message.text(), principal);
    }
}
