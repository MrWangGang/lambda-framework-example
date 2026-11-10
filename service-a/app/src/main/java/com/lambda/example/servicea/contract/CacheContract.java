package com.lambda.example.servicea.contract;

import jakarta.validation.constraints.*;

public final class CacheContract {
    private CacheContract() {}

    public record PutRequest(@NotBlank String key, @NotNull @Size(max = 10000) String value,
                             @NotBlank @Pattern(regexp = "[0-9]{1,5}") String ttlSeconds) {}
    public record KeyRequest(@NotBlank String key) {}
}
