package com.example.demo.controller;

import com.example.demo.service.CacheService;
import com.example.demo.service.ElasticsearchService;
import com.example.demo.repository.OrderRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/redis")
public class RedisCachedController {
    private static final Logger logger = LoggerFactory.getLogger(RedisCachedController.class);

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private OrderRepositoryImpl orderRepository;

    @GetMapping("/active-products/elasticsearch")
    public ResponseEntity<List<String>> getElasticsearchActiveProductsWithRedisCache() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Redis-cached Elasticsearch active products query");

        // Check Redis cache first
        List<String> cachedResult = cacheService.getElasticsearchActiveProducts();
        if (cachedResult != null) {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Redis cache hit! Returning {} products in {} ms", cachedResult.size(), duration);
            return ResponseEntity.ok(cachedResult);
        }

        // Cache miss - query Elasticsearch
        logger.info("Redis cache miss. Querying Elasticsearch directly...");
        List<String> activeProducts = elasticsearchService.findDistinctActiveProductsOptimizedForCache();

        // Cache the result
        cacheService.cacheElasticsearchActiveProducts(activeProducts);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Redis cache populated with {} active products from Elasticsearch in {} ms",
                activeProducts.size(), duration);

        return ResponseEntity.ok(activeProducts);
    }

    @GetMapping("/active-products/mongodb")
    public ResponseEntity<List<String>> getMongoDBActiveProductsWithRedisCache() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Redis-cached MongoDB active products query");

        // Check Redis cache first
        List<String> cachedResult = cacheService.getMongoDbActiveProducts();
        if (cachedResult != null) {
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Redis cache hit! Returning {} products in {} ms", cachedResult.size(), duration);
            return ResponseEntity.ok(cachedResult);
        }

        // Cache miss - query MongoDB
        logger.info("Redis cache miss. Querying MongoDB directly...");
        List<String> activeProducts = orderRepository.findDistinctActiveProductsWithMongoDistinct();

        // Cache the result
        cacheService.cacheMongoDbActiveProducts(activeProducts);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Redis cache populated with {} active products from MongoDB in {} ms",
                activeProducts.size(), duration);

        return ResponseEntity.ok(activeProducts);
    }

    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        cacheService.clearCache();
        return ResponseEntity.ok(Map.of("message", "All Redis caches have been cleared"));
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getRedisInfo() {
        LocalDateTime now = LocalDateTime.now();

        List<String> elasticsearchProducts = cacheService.getElasticsearchActiveProducts();
        List<String> mongoProducts = cacheService.getMongoDbActiveProducts();

        Map<String, Object> info = Map.of(
                "timestamp", now.toString(),
                "elasticsearch_active_products_cached", elasticsearchProducts != null ? true : false,
                "elasticsearch_active_products_count", elasticsearchProducts != null ? elasticsearchProducts.size() : 0,
                "mongodb_active_products_cached", mongoProducts != null ? true : false,
                "mongodb_active_products_count", mongoProducts != null ? mongoProducts.size() : 0);

        return ResponseEntity.ok(info);
    }
}