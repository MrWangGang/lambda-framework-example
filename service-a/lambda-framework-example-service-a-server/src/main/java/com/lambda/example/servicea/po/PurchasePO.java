package com.lambda.example.servicea.po;

import org.bson.types.ObjectId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.lambda.framework.common.po.UnifyPO;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("purchases")
@Getter
@NoArgsConstructor
public class PurchasePO extends UnifyPO<ObjectId> {
    @Indexed(name = "unique_purchase_request", unique = true)
    private String requestId;
    @Indexed(name = "purchase_user")
    private ObjectId userId;
    private ObjectId productId;
    private int quantity;
    private String unitPrice;

    public PurchasePO(ObjectId id, String requestId, ObjectId userId, ObjectId productId,
                          int quantity, String unitPrice) {
        setId(id);
        this.requestId = requestId;
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }
}
