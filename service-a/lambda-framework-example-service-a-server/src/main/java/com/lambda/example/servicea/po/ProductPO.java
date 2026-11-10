package com.lambda.example.servicea.po;

import org.bson.types.ObjectId;
import org.lambda.framework.common.enums.ConverterEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.lambda.framework.common.po.UnifyPO;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
@CompoundIndex(name = "category_recent", def = "{'category': 1, 'createdAt': -1, '_id': -1}")
@Getter
@NoArgsConstructor
public class ProductPO extends UnifyPO<ObjectId> {
    private String name;
    private String category;
    private String price;
    private int stock;
    private Status status;

    public ProductPO(ObjectId id, String name, String category, String price, int stock, Status status) {
        setId(id);
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.status = status;
    }

    public enum Status implements ConverterEnum {
        ACTIVE("在售", "商品可购买"), INACTIVE("下架", "商品已下架");

        private final String value;
        private final String description;

        Status(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() { return value; }
        public String getDescription() { return description; }
    }
}
