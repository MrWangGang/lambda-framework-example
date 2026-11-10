package com.lambda.example.servicea.controller;

import com.lambda.example.servicea.contract.SessionContract;
import com.lambda.example.servicea.contract.SystemContract;
import com.lambda.example.servicea.service.SessionServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class SessionController {
    @Resource
    private SessionServiceImpl sessions;

    @MessageMapping("/api/session/login")
    public Mono<SessionContract.LoginReply> login(@Payload SessionContract.LoginRequest input) {
        return sessions.login(input);
    }

    @MessageMapping("/api/session/whoami")
    public Mono<SessionContract.UserInfo> whoAmI(@Payload SystemContract.EmptyRequest input) {
        return sessions.whoAmI(input);
    }

    @MessageMapping("/api/session/profile")
    public Mono<SessionContract.UserInfo> profile(@Payload SystemContract.EmptyRequest input) {
        return sessions.profile(input);
    }

    @MessageMapping("/api/session/logout")
    public Mono<Void> logout(@Payload SystemContract.EmptyRequest input) {
        return sessions.logout(input);
    }
}
