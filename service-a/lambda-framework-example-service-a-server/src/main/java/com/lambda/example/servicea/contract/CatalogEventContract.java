package com.lambda.example.servicea.contract;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.bson.types.ObjectId;
import java.time.Instant;

public final class CatalogEventContract {
    private CatalogEventContract() {}

    public record PublishRequest(@NotNull ObjectId productId) {}
    public record FindRequest(@NotNull @Pattern(regexp = "[0-9]{1,19}") String eventId) {}
    public record Event(String eventId, ObjectId productId, Instant createdAt) {}
}
