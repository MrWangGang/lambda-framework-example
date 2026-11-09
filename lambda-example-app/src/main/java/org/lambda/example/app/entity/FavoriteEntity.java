package org.lambda.example.app.entity;

import jakarta.validation.constraints.NotBlank;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("favorites")
public record FavoriteEntity(@Id ObjectId id, ObjectId userId, ObjectId productId, Instant createdAt) {
    public static record ChangeDTO(@NotBlank String userId, @NotBlank String productId) {}
    public static record QueryDTO(@NotBlank String userId) {}
    public static record ViewDTO(String id, String userId, String productId, Instant createdAt) {
        public static ViewDTO from(FavoriteEntity entity) {
            return new ViewDTO(entity.id().toHexString(), entity.userId().toHexString(),
                    entity.productId().toHexString(), entity.createdAt());
        }
    }
}
