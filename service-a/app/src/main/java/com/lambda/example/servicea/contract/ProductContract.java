package com.lambda.example.servicea.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.lambda.example.servicea.po.ProductPO;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;

public final class ProductContract {
    private ProductContract() {}

    public record SaveRequest(@NotBlank @Size(max = 120) String name,
                                 @NotBlank @Size(max = 64) String category,
                                 @NotBlank @Pattern(regexp = "[0-9]{1,12}(\\.[0-9]{1,2})?") String price,
                                 @NotBlank @Pattern(regexp = "[0-9]{1,8}") String stock,
                                 @NotNull ProductPO.Status status) {}

    public record FindRequest(@NotNull ObjectId id) {}

    public record UpdateRequest(@NotNull ObjectId id,
                                   @NotBlank @Pattern(regexp = "[0-9]{1,18}") String version,
                                   @NotNull @Valid SaveRequest product) {}

    public record CursorRequest(@NotBlank @Size(max = 64) String category,
                                   @Pattern(regexp = "[0-9]{1,3}") String limit,
                                   Instant beforeTime, ObjectId beforeId) {}

    public record PageRequest(@NotBlank @Size(max = 64) String category,
                                 @Pattern(regexp = "[1-9][0-9]{0,4}") String page,
                                 @Pattern(regexp = "[1-9][0-9]{0,2}") String size) {}

    public record Detail(ObjectId id, String name, String category, String price,
                                 String stock, String version, Instant createdAt, ProductPO.Status status) {
        public static Detail from(ProductPO entity) {
            return new Detail(entity.getId(), entity.getName(), entity.getCategory(), entity.getPrice(),
                    Integer.toString(entity.getStock()), Long.toString(entity.getVersion()),
                    Instant.ofEpochMilli(entity.getCreatedAt().toEpochMilli()), entity.getStatus());
        }
    }

    public record CursorResult(List<Detail> items, Instant nextBeforeTime, ObjectId nextBeforeId) {}
}
