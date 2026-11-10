package com.lambda.example.servicea.contract;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class VectorContract {
    private VectorContract() {}

    public record UpsertRequest(@NotNull UUID id, @NotNull @Size(min = 3, max = 3) List<@NotNull Float> vector,
                                @NotBlank @Size(max = 64) String category) {}
    public record QueryRequest(@NotNull @Size(min = 3, max = 3) List<@NotNull Float> vector,
                               @NotBlank @Pattern(regexp = "(?:[1-9][0-9]?|100)") String limit) {}
    public record Hit(String id, float score, String category) {}
}
