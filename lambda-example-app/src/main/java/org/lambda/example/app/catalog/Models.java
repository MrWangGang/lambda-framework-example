package org.lambda.example.app.catalog;

import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Models {
    private Models() {}
    @Document("products")
    public record Product(@Id String id, String name, String category,
            @Field(targetType = FieldType.DECIMAL128) BigDecimal price,
            int stock, @Version Long version, Instant createdAt) {}
    public record ProductInput(@NotBlank @Size(max=120) String name,
            @NotBlank @Size(max=64) String category,
            @NotNull @DecimalMin("0.00") @Digits(integer=12, fraction=2) BigDecimal price,
            @Min(0) int stock) {}
    public record ProductPage(List<Product> items, Instant nextBeforeTime, String nextBeforeId) {}
    @Document("favorites")
    public record Favorite(@Id String id, String userId, String productId, Instant createdAt) {}
    public record PurchaseInput(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,80}") String requestId,
            @NotBlank @Size(max=80) String userId, @NotBlank String productId, @Min(1) @Max(1000) int quantity) {}
    @Document("purchases")
    public record Purchase(@Id String id, String userId, String productId, int quantity,
            @Field(targetType = FieldType.DECIMAL128) BigDecimal unitPrice, Instant createdAt) {}
}
