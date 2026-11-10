package com.lambda.example.servicea.contract;

import jakarta.validation.constraints.NotNull;
import com.lambda.example.servicea.po.FavoritePO;
import org.bson.types.ObjectId;

import java.time.Instant;

public final class FavoriteContract {
    private FavoriteContract() {}

    public record ChangeRequest(@NotNull ObjectId userId, @NotNull ObjectId productId) {}
    public record QueryRequest(@NotNull ObjectId userId) {}
    public record Detail(ObjectId id, ObjectId userId,
                                 ObjectId productId, Instant createdAt) {
        public static Detail from(FavoritePO entity) {
            return new Detail(entity.getId(), entity.getUserId(), entity.getProductId(), entity.getCreatedAt());
        }
    }
}
