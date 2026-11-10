package com.lambda.example.servicea.po;

import org.bson.types.ObjectId;
import lombok.Getter;
import org.lambda.framework.common.po.UnifyPO;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("delivery_receipts")
@Getter
public class DeliveryReceiptPO extends UnifyPO<ObjectId> {
    private String content;
    private Instant receivedAt;
}
