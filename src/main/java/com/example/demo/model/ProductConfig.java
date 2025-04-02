package com.example.demo.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "product_configs")
public class ProductConfig {
    @Id
    private String id;
    private String productId;
    private boolean enabled;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}