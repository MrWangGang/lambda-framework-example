package com.lambda.example.servicea.contract;

import jakarta.validation.constraints.*;
import com.lambda.example.servicea.po.PurchasePO;
import org.bson.types.ObjectId;

import java.time.Instant;

public final class PurchaseContract {
    private PurchaseContract() {}

    public record CreateRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,80}") String requestId,
                                   @NotNull ObjectId userId,
                                   @NotNull ObjectId productId,
                                   @NotBlank @Pattern(regexp = "(?:[1-9][0-9]{0,2}|1000)") String quantity) {}
    public record SummaryRequest(@NotNull ObjectId userId) {}
    public record CategoryTotal(String category, String purchases, String units) {}

    public record Detail(ObjectId id, String requestId, ObjectId userId,
                                 ObjectId productId,
                                 String quantity, String unitPrice, Instant createdAt) {
        public static Detail from(PurchasePO entity) {
            return new Detail(entity.getId(), entity.getRequestId(), entity.getUserId(),
                    entity.getProductId(), Integer.toString(entity.getQuantity()),
                    entity.getUnitPrice(), entity.getCreatedAt());
        }
    }
}
