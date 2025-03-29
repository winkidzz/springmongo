package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "product_configs")
public class ProductConfig {
    @Id
    private String id;
    private String productId;
    private String configName;
    private String configValue;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean enabled;
}