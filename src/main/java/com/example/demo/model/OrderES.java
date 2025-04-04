package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.example.demo.config.FlexibleLocalDateTimeDeserializer;

import lombok.Data;
import java.time.LocalDateTime;

@Data
@Document(indexName = "orders")
public class OrderES {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String orderId;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime orderDate;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String productId;

    @Field(type = FieldType.Keyword)
    private String customerId;

    @Field(type = FieldType.Double)
    private double amount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss[.SSS]")
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    // Constructor to convert from MongoDB Order to Elasticsearch OrderES
    public static OrderES fromOrder(Order order) {
        OrderES orderES = new OrderES();
        orderES.setId(order.getId());
        orderES.setOrderId(order.getOrderId());
        orderES.setOrderDate(order.getOrderDate());
        orderES.setStatus(order.getStatus());
        orderES.setProductId(order.getProductId());
        orderES.setCustomerId(order.getCustomerId());
        orderES.setAmount(order.getAmount());
        orderES.setCreatedAt(order.getCreatedAt());
        orderES.setUpdatedAt(order.getUpdatedAt());
        return orderES;
    }
}