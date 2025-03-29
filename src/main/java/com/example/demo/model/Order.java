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
    private String orderNumber;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDate;
    private String productName;
    private String productCategory;
    private Double price;
    private Integer quantity;
    private String status;
}