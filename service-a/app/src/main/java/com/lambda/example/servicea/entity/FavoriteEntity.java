package com.lambda.example.servicea.entity;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("favorites")
public record FavoriteEntity(@Id ObjectId id, ObjectId userId, ObjectId productId, Instant createdAt) {
    public static record ChangeDTO(@NotNull ObjectId userId, @NotNull ObjectId productId) {}
    public static record QueryDTO(@NotNull ObjectId userId) {}
    public static record ViewDTO(ObjectId id, ObjectId userId,
                                 ObjectId productId, Instant createdAt) {
        public static ViewDTO from(FavoriteEntity entity) {
            return new ViewDTO(entity.id(), entity.userId(), entity.productId(), entity.createdAt());
        }
    }
}
