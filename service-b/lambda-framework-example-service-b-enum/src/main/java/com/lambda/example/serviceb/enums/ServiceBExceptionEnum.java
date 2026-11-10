package com.lambda.example.serviceb.enums;

import lombok.Getter;
import org.lambda.framework.enums.ExceptionEnumFunction;

@Getter
public enum ServiceBExceptionEnum implements ExceptionEnumFunction {
    CHAT_MESSAGE_REQUIRED("B0900001", "请至少发送一条消息");

    private final String code;
    private final String message;

    ServiceBExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
