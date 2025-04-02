package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(indexName = "product_configs")
public class ProductConfigES {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String productId;

    @Field(type = FieldType.Boolean)
    private boolean enabled;

    @Field(type = FieldType.Date)
    private LocalDateTime startDate;

    @Field(type = FieldType.Date)
    private LocalDateTime endDate;

    // Constructor to convert from MongoDB ProductConfig to Elasticsearch
    // ProductConfigES
    public static ProductConfigES fromProductConfig(ProductConfig config) {
        ProductConfigES configES = new ProductConfigES();
        configES.setId(config.getId());
        configES.setProductId(config.getProductId());
        configES.setEnabled(config.isEnabled());
        configES.setStartDate(config.getStartDate());
        configES.setEndDate(config.getEndDate());
        return configES;
    }
}