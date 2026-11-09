package org.lambda.example.app.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.bson.types.ObjectId;
import org.lambda.framework.common.jackson.JsonObjectId;
import org.lambda.framework.common.enums.ConverterEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("products")
public record ProductEntity(@Id ObjectId id, String name, String category, String price,
                            int stock, @Version Long version, Instant createdAt, Status status) {
    public enum Status implements ConverterEnum {
        ACTIVE("active", "在售"), INACTIVE("inactive", "下架");

        private final String value;
        private final String description;

        Status(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }
    }

    public static record SaveDTO(@NotBlank @Size(max = 120) String name,
                                 @NotBlank @Size(max = 64) String category,
                                 @NotBlank @Pattern(regexp = "[0-9]{1,12}(\\.[0-9]{1,2})?") String price,
                                 @NotBlank @Pattern(regexp = "[0-9]{1,8}") String stock,
                                 @NotNull Status status) {}

    public static record IdDTO(@JsonObjectId @NotNull ObjectId id) {}

    public static record UpdateDTO(@JsonObjectId @NotNull ObjectId id,
                                   @NotBlank @Pattern(regexp = "[0-9]{1,18}") String version,
                                   @NotNull @Valid SaveDTO product) {}

    public static record CursorDTO(@NotBlank @Size(max = 64) String category,
                                   @Pattern(regexp = "[0-9]{1,3}") String limit,
                                   Instant beforeTime, @JsonObjectId ObjectId beforeId) {}

    public static record PageDTO(@NotBlank @Size(max = 64) String category,
                                 @Pattern(regexp = "[1-9][0-9]{0,4}") String page,
                                 @Pattern(regexp = "[1-9][0-9]{0,2}") String size) {}

    public static record ViewDTO(@JsonObjectId ObjectId id, String name, String category, String price,
                                 String stock, String version, Instant createdAt, Status status) {
        public static ViewDTO from(ProductEntity entity) {
            return new ViewDTO(entity.id(), entity.name(), entity.category(), entity.price(),
                    Integer.toString(entity.stock()), Long.toString(entity.version()),
                    Instant.ofEpochMilli(entity.createdAt().toEpochMilli()), entity.status());
        }
    }

    public static record CursorResultDTO(List<ViewDTO> items, Instant nextBeforeTime, @JsonObjectId ObjectId nextBeforeId) {}
}
