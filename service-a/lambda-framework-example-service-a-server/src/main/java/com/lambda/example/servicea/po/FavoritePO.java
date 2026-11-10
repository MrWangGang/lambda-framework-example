package com.lambda.example.servicea.po;

import org.bson.types.ObjectId;
import lombok.Getter;
import org.lambda.framework.common.po.UnifyPO;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("favorites")
@CompoundIndex(name = "unique_user_product", def = "{'userId': 1, 'productId': 1}", unique = true)
@CompoundIndex(name = "user_recent", def = "{'userId': 1, 'createdAt': -1, '_id': -1}")
@CompoundIndex(name = "product_recent", def = "{'productId': 1, 'createdAt': -1}")
@Getter
public class FavoritePO extends UnifyPO<ObjectId> {
    private ObjectId userId;
    private ObjectId productId;
}
