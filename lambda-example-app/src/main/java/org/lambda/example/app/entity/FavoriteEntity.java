package org.lambda.example.app.entity;

import jakarta.validation.constraints.NotNull;
import org.bson.types.ObjectId;
import org.lambda.framework.common.jackson.JsonObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("favorites")
public record FavoriteEntity(@Id ObjectId id, ObjectId userId, ObjectId productId, Instant createdAt) {
    public static record ChangeDTO(@JsonObjectId @NotNull ObjectId userId, @JsonObjectId @NotNull ObjectId productId) {}
    public static record QueryDTO(@JsonObjectId @NotNull ObjectId userId) {}
    public static record ViewDTO(@JsonObjectId ObjectId id, @JsonObjectId ObjectId userId,
                                 @JsonObjectId ObjectId productId, Instant createdAt) {
        public static ViewDTO from(FavoriteEntity entity) {
            return new ViewDTO(entity.id(), entity.userId(), entity.productId(), entity.createdAt());
        }
    }
}
