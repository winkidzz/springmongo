package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@RedisHash("product_config")
public class ProductConfigRedis implements Serializable {

    @Id
    private String id;

    @Indexed
    private String productId;

    @Indexed
    private boolean enabled;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    // Factory method to convert from MongoDB ProductConfig
    public static ProductConfigRedis fromProductConfig(ProductConfig config) {
        ProductConfigRedis redisConfig = new ProductConfigRedis();
        redisConfig.setId(config.getId());
        redisConfig.setProductId(config.getProductId());
        redisConfig.setEnabled(config.isEnabled());
        redisConfig.setStartDate(config.getStartDate());
        redisConfig.setEndDate(config.getEndDate());
        return redisConfig;
    }

    // Factory method to convert from Elasticsearch ProductConfigES
    public static ProductConfigRedis fromProductConfigES(ProductConfigES config) {
        ProductConfigRedis redisConfig = new ProductConfigRedis();
        redisConfig.setId(config.getId());
        redisConfig.setProductId(config.getProductId());
        redisConfig.setEnabled(config.isEnabled());
        redisConfig.setStartDate(config.getStartDate());
        redisConfig.setEndDate(config.getEndDate());
        return redisConfig;
    }
}