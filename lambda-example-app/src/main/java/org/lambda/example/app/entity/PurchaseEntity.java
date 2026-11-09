package org.lambda.example.app.entity;

import jakarta.validation.constraints.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("purchases")
public record PurchaseEntity(@Id ObjectId id, String requestId, ObjectId userId, ObjectId productId,
                             int quantity, String unitPrice, Instant createdAt) {
    public static record CreateDTO(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String requestId,
                                   @NotBlank String userId,
                                   @NotBlank String productId,
                                   @NotBlank @Pattern(regexp = "(?:[1-9][0-9]{0,2}|1000)") String quantity) {}
    public static record ViewDTO(String id, String requestId, String userId, String productId,
                                 String quantity, String unitPrice, Instant createdAt) {
        public static ViewDTO from(PurchaseEntity entity) {
            return new ViewDTO(entity.id().toHexString(), entity.requestId(), entity.userId().toHexString(),
                    entity.productId().toHexString(), Integer.toString(entity.quantity()),
                    entity.unitPrice(), entity.createdAt());
        }
    }
}
