package org.lambda.example.app.entity;

import jakarta.validation.constraints.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("delivery_receipts")
public record DeliveryReceiptEntity(@Id ObjectId id, String content, Instant createdAt, Instant receivedAt) {
    public static record SendDTO(@NotBlank @Size(max = 10000) String content,
                                 @NotBlank @Pattern(regexp = "[0-9]{1,5}") String delayMillis) {}

    public static record IdDTO(@NotBlank String id) {}

    public static record MessageDTO(String id, String content, Instant createdAt) {
        public MessageDTO {
            if (!ObjectId.isValid(id) || content == null || content.isBlank()
                    || content.length() > 10000 || createdAt == null) {
                throw new IllegalArgumentException("Invalid notification fields");
            }
        }
    }

    public static record ViewDTO(String id, String content, Instant createdAt, Instant receivedAt) {
        public static ViewDTO from(DeliveryReceiptEntity entity) {
            return new ViewDTO(entity.id().toHexString(), entity.content(), entity.createdAt(), entity.receivedAt());
        }
    }
}
