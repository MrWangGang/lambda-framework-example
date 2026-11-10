package com.lambda.example.servicea.contract;

import jakarta.validation.constraints.NotBlank;

public final class SessionContract {
    private SessionContract() {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginReply(String token) {}
    public record UserInfo(String id) {}
}
