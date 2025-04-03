package com.example.demo.service;

import com.example.demo.model.ProductConfig;
import com.example.demo.model.ProductConfigES;
import com.example.demo.model.ProductConfigRedis;
import com.example.demo.repository.ProductConfigESRepository;
import com.example.demo.repository.ProductConfigRedisRepository;
import com.example.demo.repository.ProductConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class RedisProductService {
    private static final Logger logger = LoggerFactory.getLogger(RedisProductService.class);

    private final ProductConfigRedisRepository redisRepository;
    private final ProductConfigRepository mongoRepository;
    private final ProductConfigESRepository elasticsearchRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisProductService(
            ProductConfigRedisRepository redisRepository,
            ProductConfigRepository mongoRepository,
            ProductConfigESRepository elasticsearchRepository,
            RedisTemplate<String, Object> redisTemplate) {
        this.redisRepository = redisRepository;
        this.mongoRepository = mongoRepository;
        this.elasticsearchRepository = elasticsearchRepository;
        this.redisTemplate = redisTemplate;
    }

    // Get all product configurations from Redis
    public List<ProductConfigRedis> getAllProductConfigs() {
        return StreamSupport
                .stream(redisRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    // Get a specific product configuration by ID
    public Optional<ProductConfigRedis> getProductConfigById(String id) {
        return redisRepository.findById(id);
    }

    // Get configurations for a specific product
    public List<ProductConfigRedis> getProductConfigsByProductId(String productId) {
        return redisRepository.findByProductId(productId);
    }

    // Get all active product configurations (that are valid for the current time)
    public List<ProductConfigRedis> getActiveProductConfigs() {
        LocalDateTime now = LocalDateTime.now();
        return redisRepository.findActiveConfigurations(now);
    }

    // Get distinct active product IDs
    public List<String> getDistinctActiveProductIds() {
        LocalDateTime now = LocalDateTime.now();
        return redisRepository.findActiveConfigurations(now).stream()
                .map(ProductConfigRedis::getProductId)
                .distinct()
                .collect(Collectors.toList());
    }

    // Save a new product configuration
    public ProductConfigRedis saveProductConfig(ProductConfigRedis config) {
        return redisRepository.save(config);
    }

    // Update an existing product configuration
    public ProductConfigRedis updateProductConfig(String id, ProductConfigRedis config) {
        config.setId(id);
        return redisRepository.save(config);
    }

    // Delete a product configuration
    public void deleteProductConfig(String id) {
        redisRepository.deleteById(id);
    }

    // Delete all configurations for a product
    public void deleteProductConfigurations(String productId) {
        redisRepository.deleteByProductId(productId);
    }

    // Synchronize data from MongoDB to Redis
    public int syncFromMongoDB() {
        logger.info("Starting synchronization from MongoDB to Redis");
        List<ProductConfig> mongoConfigs = mongoRepository.findAll();
        int count = 0;

        for (ProductConfig mongoConfig : mongoConfigs) {
            ProductConfigRedis redisConfig = ProductConfigRedis.fromProductConfig(mongoConfig);
            redisRepository.save(redisConfig);
            count++;
        }

        logger.info("Synchronized {} product configurations from MongoDB to Redis", count);
        return count;
    }

    // Synchronize data from Elasticsearch to Redis
    public int syncFromElasticsearch() {
        logger.info("Starting synchronization from Elasticsearch to Redis");
        Iterable<ProductConfigES> esConfigs = elasticsearchRepository.findAll();
        int count = 0;

        for (ProductConfigES esConfig : esConfigs) {
            ProductConfigRedis redisConfig = ProductConfigRedis.fromProductConfigES(esConfig);
            redisRepository.save(redisConfig);
            count++;
        }

        logger.info("Synchronized {} product configurations from Elasticsearch to Redis", count);
        return count;
    }

    // Clear all product configurations from Redis
    public void clearAllProductConfigs() {
        redisRepository.deleteAll();
        logger.info("Cleared all product configurations from Redis");
    }
}