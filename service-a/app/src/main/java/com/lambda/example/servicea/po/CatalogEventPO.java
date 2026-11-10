package com.lambda.example.servicea.po;

import lombok.Getter;
import org.bson.types.ObjectId;
import org.lambda.framework.common.po.UnifyPO;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document("catalog_events")
public class CatalogEventPO extends UnifyPO<ObjectId> {
    @Indexed(name = "unique_catalog_event", unique = true)
    private String eventId;
    private ObjectId productId;
}
