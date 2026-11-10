package com.lambda.example.servicea.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.lambda.framework.common.exception.basic.GlobalException;
import org.lambda.framework.common.po.SecurityLoginUser;
import org.lambda.framework.compliance.security.PrincipalFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Controller
public class SessionController {
    private final PrincipalFactory principals;
    private final byte[] demoPassword;
    private final String demoUser;

    public SessionController(PrincipalFactory principals, @Value("${example.demo-password}") String demoPassword,
                             @Value("${example.demo-user:demo}") String demoUser) {
        if (demoPassword.isBlank() || !demoUser.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Configure a demo login password");
        }
        this.principals = principals;
        this.demoPassword = demoPassword.getBytes(StandardCharsets.UTF_8);
        this.demoUser = demoUser;
    }

    @MessageMapping("/api/session/login")
    public Mono<LoginReply> login(@Valid @Payload LoginRequest request) {
        if (!demoUser.equals(request.username()) || !MessageDigest.isEqual(
                demoPassword, request.password().getBytes(StandardCharsets.UTF_8))) {
            return Mono.error(new GlobalException("EXAMPLE_LOGIN_001", "用户名或口令错误"));
        }
        var user = new DemoUser();
        user.setId(demoUser);
        return principals.setPrincipal(user).map(LoginReply::new);
    }

    @MessageMapping("/api/session/whoami")
    public Mono<UserInfo> whoAmI(@Payload EmptyRequest input) {
        return principals.currentUserId().map(UserInfo::new);
    }

    @MessageMapping("/api/session/profile")
    public Mono<UserInfo> profile(@Payload EmptyRequest input) {
        return principals.currentUser(DemoUser.class).map(user -> new UserInfo(user.getId()));
    }

    @MessageMapping("/api/session/logout")
    public Mono<Void> logout(@Payload EmptyRequest input) {
        return principals.deletePrincipal();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginReply(String token) {}
    public record EmptyRequest() {}
    public record UserInfo(String id) {}

    public static class DemoUser implements SecurityLoginUser<String> {
        private String id;
        @Override public String getId() { return id; }
        @Override public void setId(String id) { this.id = id; }
    }
}
