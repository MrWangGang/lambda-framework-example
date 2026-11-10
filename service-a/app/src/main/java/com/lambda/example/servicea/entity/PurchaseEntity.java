package com.lambda.example.servicea.entity;

import jakarta.validation.constraints.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("purchases")
public record PurchaseEntity(@Id ObjectId id, String requestId, ObjectId userId, ObjectId productId,
                             int quantity, String unitPrice, Instant createdAt) {
    public static record CreateDTO(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String requestId,
                                   @NotNull ObjectId userId,
                                   @NotNull ObjectId productId,
                                   @NotBlank @Pattern(regexp = "(?:[1-9][0-9]{0,2}|1000)") String quantity) {}
    public static record ViewDTO(ObjectId id, String requestId, ObjectId userId,
                                 ObjectId productId,
                                 String quantity, String unitPrice, Instant createdAt) {
        public static ViewDTO from(PurchaseEntity entity) {
            return new ViewDTO(entity.id(), entity.requestId(), entity.userId(),
                    entity.productId(), Integer.toString(entity.quantity()),
                    entity.unitPrice(), entity.createdAt());
        }
    }
}
