package com.example.demo.controller;

import com.example.demo.model.ProductConfig;
import com.example.demo.service.ProductConfigDualWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API controller for active product configurations.
 * Provides endpoints for retrieving active products and managing
 * product configurations with dual-write capability.
 */
@RestController
@RequestMapping("/api/products")
public class ActiveProductApiController {
    private static final Logger logger = LoggerFactory.getLogger(ActiveProductApiController.class);

    private final ProductConfigDualWriteService dualWriteService;

    @Autowired
    public ActiveProductApiController(ProductConfigDualWriteService dualWriteService) {
        this.dualWriteService = dualWriteService;
    }

    /**
     * Returns a list of active product IDs.
     * Uses Redis for ultra-fast retrieval with automatic fallback to MongoDB.
     */
    @GetMapping("/active")
    public ResponseEntity<List<String>> getActiveProductIds() {
        long startTime = System.currentTimeMillis();
        logger.info("Fetching active product IDs");

        List<String> activeProductIds = dualWriteService.getDistinctActiveProductIds();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Retrieved {} active product IDs in {} ms", activeProductIds.size(), duration);

        return ResponseEntity.ok(activeProductIds);
    }

    /**
     * Returns details about a specific product configuration.
     */
    @GetMapping("/config/{id}")
    public ResponseEntity<ProductConfig> getProductConfig(@PathVariable String id) {
        logger.info("Fetching product configuration with ID: {}", id);

        Optional<ProductConfig> config = dualWriteService.getProductConfigById(id);

        return config.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new product configuration with dual-write to MongoDB and Redis.
     */
    @PostMapping("/config")
    public ResponseEntity<ProductConfig> createProductConfig(@RequestBody ProductConfig config) {
        logger.info("Creating new product configuration for product: {}", config.getProductId());

        ProductConfig savedConfig = dualWriteService.createProductConfig(config);

        return ResponseEntity.ok(savedConfig);
    }

    /**
     * Updates an existing product configuration with dual-write to MongoDB and
     * Redis.
     */
    @PutMapping("/config/{id}")
    public ResponseEntity<ProductConfig> updateProductConfig(
            @PathVariable String id,
            @RequestBody ProductConfig config) {
        logger.info("Updating product configuration with ID: {}", id);

        ProductConfig updatedConfig = dualWriteService.updateProductConfig(id, config);

        return ResponseEntity.ok(updatedConfig);
    }

    /**
     * Deletes a product configuration from both MongoDB and Redis.
     */
    @DeleteMapping("/config/{id}")
    public ResponseEntity<Map<String, String>> deleteProductConfig(@PathVariable String id) {
        logger.info("Deleting product configuration with ID: {}", id);

        dualWriteService.deleteProductConfig(id);

        return ResponseEntity.ok(Collections.singletonMap("message",
                "Product configuration successfully deleted from MongoDB and Redis"));
    }

    /**
     * Force synchronization of product configurations from MongoDB to Redis.
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncMongoToRedis() {
        logger.info("Manually triggering MongoDB to Redis synchronization");

        long startTime = System.currentTimeMillis();
        int count = dualWriteService.syncFromMongoToRedis();
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
                "message", "Synchronized product configurations from MongoDB to Redis",
                "count", count,
                "duration_ms", duration));
    }

    /**
     * Check consistency between MongoDB and Redis.
     */
    @GetMapping("/consistency-check")
    public ResponseEntity<Map<String, Object>> checkConsistency() {
        logger.info("Running consistency check between MongoDB and Redis");

        long startTime = System.currentTimeMillis();
        boolean consistent = dualWriteService.verifyConsistency();
        long duration = System.currentTimeMillis() - startTime;

        return ResponseEntity.ok(Map.of(
                "consistent", consistent,
                "check_time", LocalDateTime.now().toString(),
                "duration_ms", duration));
    }
}