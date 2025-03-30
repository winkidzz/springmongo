package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String productId;
    private String productConfigId;
    private int quantity;
    private double price;
    private String status;
    private LocalDateTime orderDate;
    private String orderNumber;
    private LocalDateTime deliveryDate;
    private String productName;
    private String productCategory;
}