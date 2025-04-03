package com.example.demo.controller;

import com.example.demo.model.ProductConfigRedis;
import com.example.demo.service.RedisProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/redis-db")
public class RedisProductController {
    private static final Logger logger = LoggerFactory.getLogger(RedisProductController.class);

    private final RedisProductService redisProductService;

    @Autowired
    public RedisProductController(RedisProductService redisProductService) {
        this.redisProductService = redisProductService;
    }

    @GetMapping("/configs")
    public ResponseEntity<List<ProductConfigRedis>> getAllProductConfigs() {
        long startTime = System.currentTimeMillis();
        List<ProductConfigRedis> configs = redisProductService.getAllProductConfigs();
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved {} product configurations from Redis in {} ms", configs.size(), duration);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/configs/{id}")
    public ResponseEntity<ProductConfigRedis> getProductConfigById(@PathVariable String id) {
        Optional<ProductConfigRedis> config = redisProductService.getProductConfigById(id);
        return config.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products/{productId}/configs")
    public ResponseEntity<List<ProductConfigRedis>> getProductConfigsByProductId(@PathVariable String productId) {
        List<ProductConfigRedis> configs = redisProductService.getProductConfigsByProductId(productId);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/active-configs")
    public ResponseEntity<List<ProductConfigRedis>> getActiveProductConfigs() {
        long startTime = System.currentTimeMillis();
        List<ProductConfigRedis> configs = redisProductService.getActiveProductConfigs();
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved {} active product configurations from Redis in {} ms", configs.size(), duration);
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/active-products")
    public ResponseEntity<List<String>> getDistinctActiveProductIds() {
        long startTime = System.currentTimeMillis();
        List<String> productIds = redisProductService.getDistinctActiveProductIds();
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved {} distinct active product IDs from Redis in {} ms", productIds.size(), duration);
        return ResponseEntity.ok(productIds);
    }

    @PostMapping("/configs")
    public ResponseEntity<ProductConfigRedis> createProductConfig(@RequestBody ProductConfigRedis config) {
        ProductConfigRedis savedConfig = redisProductService.saveProductConfig(config);
        return ResponseEntity.ok(savedConfig);
    }

    @PutMapping("/configs/{id}")
    public ResponseEntity<ProductConfigRedis> updateProductConfig(
            @PathVariable String id,
            @RequestBody ProductConfigRedis config) {
        ProductConfigRedis updatedConfig = redisProductService.updateProductConfig(id, config);
        return ResponseEntity.ok(updatedConfig);
    }

    @DeleteMapping("/configs/{id}")
    public ResponseEntity<Map<String, String>> deleteProductConfig(@PathVariable String id) {
        redisProductService.deleteProductConfig(id);
        return ResponseEntity.ok(Map.of("message", "Product configuration deleted successfully"));
    }

    @DeleteMapping("/products/{productId}/configs")
    public ResponseEntity<Map<String, String>> deleteProductConfigurations(@PathVariable String productId) {
        redisProductService.deleteProductConfigurations(productId);
        return ResponseEntity
                .ok(Map.of("message", "All configurations for product " + productId + " deleted successfully"));
    }

    @PostMapping("/sync/mongodb")
    public ResponseEntity<Map<String, Object>> syncFromMongoDB() {
        long startTime = System.currentTimeMillis();
        int count = redisProductService.syncFromMongoDB();
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
                "message", "Synchronized product configurations from MongoDB to Redis",
                "count", count,
                "duration_ms", duration));
    }

    @PostMapping("/sync/elasticsearch")
    public ResponseEntity<Map<String, Object>> syncFromElasticsearch() {
        long startTime = System.currentTimeMillis();
        int count = redisProductService.syncFromElasticsearch();
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
                "message", "Synchronized product configurations from Elasticsearch to Redis",
                "count", count,
                "duration_ms", duration));
    }

    @DeleteMapping("/configs")
    public ResponseEntity<Map<String, String>> clearAllProductConfigs() {
        redisProductService.clearAllProductConfigs();
        return ResponseEntity.ok(Map.of("message", "All product configurations cleared from Redis"));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        long startTime = System.currentTimeMillis();
        int totalConfigs = redisProductService.getAllProductConfigs().size();
        int activeConfigs = redisProductService.getActiveProductConfigs().size();
        int distinctActiveProducts = redisProductService.getDistinctActiveProductIds().size();
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
                "total_configurations", totalConfigs,
                "active_configurations", activeConfigs,
                "distinct_active_products", distinctActiveProducts,
                "current_time", LocalDateTime.now().toString(),
                "query_time_ms", duration));
    }
}