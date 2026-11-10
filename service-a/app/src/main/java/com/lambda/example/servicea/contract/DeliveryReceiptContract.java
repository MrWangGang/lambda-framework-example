package com.lambda.example.servicea.contract;

import jakarta.validation.constraints.*;
import com.lambda.example.servicea.po.DeliveryReceiptPO;
import org.bson.types.ObjectId;

import java.time.Instant;

public final class DeliveryReceiptContract {
    private DeliveryReceiptContract() {}

    public record SendRequest(@NotBlank @Size(max = 10000) String content,
                                 @NotBlank @Pattern(regexp = "[0-9]{1,5}") String delayMillis) {}

    public record FindRequest(@NotNull ObjectId id) {}

    public record Message(ObjectId id, String content, Instant createdAt) {
        public Message {
            if (id == null || content == null || content.isBlank()
                    || content.length() > 10000 || createdAt == null) {
                throw new IllegalArgumentException("Invalid notification fields");
            }
        }
    }

    public record Detail(ObjectId id, String content, Instant createdAt, Instant receivedAt) {
        public static Detail from(DeliveryReceiptPO entity) {
            return new Detail(entity.getId(), entity.getContent(), entity.getCreatedAt(), entity.getReceivedAt());
        }
    }
}
