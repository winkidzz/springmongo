package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    private static final String ELASTICSEARCH_ACTIVE_PRODUCTS_KEY = "es:active-products";
    private static final String MONGODB_ACTIVE_PRODUCTS_KEY = "mongo:active-products";
    private static final long DEFAULT_TTL = 60; // seconds

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheElasticsearchActiveProducts(List<String> activeProducts) {
        try {
            logger.info("Caching {} Elasticsearch active products in Redis", activeProducts.size());
            redisTemplate.opsForValue().set(ELASTICSEARCH_ACTIVE_PRODUCTS_KEY, activeProducts, DEFAULT_TTL,
                    TimeUnit.SECONDS);
            logger.info("Successfully cached Elasticsearch active products");
        } catch (Exception e) {
            logger.error("Error caching Elasticsearch active products: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getElasticsearchActiveProducts() {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(ELASTICSEARCH_ACTIVE_PRODUCTS_KEY);
            if (cachedValue != null) {
                logger.info("Cache hit for Elasticsearch active products");
                return (List<String>) cachedValue;
            }
            logger.info("Cache miss for Elasticsearch active products");
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving Elasticsearch active products from cache: {}", e.getMessage(), e);
            return null;
        }
    }

    public void cacheMongoDbActiveProducts(List<String> activeProducts) {
        try {
            logger.info("Caching {} MongoDB active products in Redis", activeProducts.size());
            redisTemplate.opsForValue().set(MONGODB_ACTIVE_PRODUCTS_KEY, activeProducts, DEFAULT_TTL, TimeUnit.SECONDS);
            logger.info("Successfully cached MongoDB active products");
        } catch (Exception e) {
            logger.error("Error caching MongoDB active products: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> getMongoDbActiveProducts() {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(MONGODB_ACTIVE_PRODUCTS_KEY);
            if (cachedValue != null) {
                logger.info("Cache hit for MongoDB active products");
                return (List<String>) cachedValue;
            }
            logger.info("Cache miss for MongoDB active products");
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving MongoDB active products from cache: {}", e.getMessage(), e);
            return null;
        }
    }

    public void clearCache() {
        try {
            logger.info("Clearing all caches");
            redisTemplate.delete(ELASTICSEARCH_ACTIVE_PRODUCTS_KEY);
            redisTemplate.delete(MONGODB_ACTIVE_PRODUCTS_KEY);
            logger.info("Successfully cleared all caches");
        } catch (Exception e) {
            logger.error("Error clearing caches: {}", e.getMessage(), e);
        }
    }
}