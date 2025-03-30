package com.example.demo.controller;

import com.example.demo.model.ProductConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/configs")
public class ConfigController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping
    public ResponseEntity<List<ProductConfig>> getAllConfigs() {
        List<ProductConfig> configs = mongoTemplate.findAll(ProductConfig.class, "product_configs");
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProductConfig>> getActiveConfigs() {
        LocalDateTime now = LocalDateTime.now();
        List<ProductConfig> activeConfigs = mongoTemplate.find(
            Query.query(Criteria.where("enabled").is(true)
                .and("releaseStartDate").lte(now)
                .and("releaseEndDate").gte(now)),
            ProductConfig.class,
            "product_configs"
        );
        return ResponseEntity.ok(activeConfigs);
    }
} 