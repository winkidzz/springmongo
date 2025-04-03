package com.example.demo.service;

import com.example.demo.model.ProductConfig;
import com.example.demo.model.ProductConfigRedis;
import com.example.demo.repository.ProductConfigRepository;
import com.example.demo.repository.ProductConfigRedisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Service that maintains consistency between MongoDB and Redis for product
 * configurations.
 * MongoDB is considered the source of truth, with Redis serving as a
 * high-performance access layer.
 */
@Service
public class ProductConfigDualWriteService {
    private static final Logger logger = LoggerFactory.getLogger(ProductConfigDualWriteService.class);

    private final ProductConfigRepository mongoRepository;
    private final ProductConfigRedisRepository redisRepository;

    @Autowired
    public ProductConfigDualWriteService(
            ProductConfigRepository mongoRepository,
            ProductConfigRedisRepository redisRepository) {
        this.mongoRepository = mongoRepository;
        this.redisRepository = redisRepository;
    }

    /**
     * Creates a new product configuration in both MongoDB and Redis.
     * MongoDB write is performed first, as it's the source of truth.
     */
    @Transactional
    public ProductConfig createProductConfig(ProductConfig config) {
        logger.info("Creating new product configuration for product ID: {}", config.getProductId());

        // Write to MongoDB first (source of truth)
        ProductConfig savedConfig = mongoRepository.save(config);

        try {
            // Then write to Redis
            ProductConfigRedis redisConfig = ProductConfigRedis.fromProductConfig(savedConfig);
            redisRepository.save(redisConfig);
            logger.debug("Successfully wrote product configuration to both MongoDB and Redis");
        } catch (Exception e) {
            logger.error("Failed to write to Redis, but MongoDB write succeeded: {}", e.getMessage());
            // MongoDB write succeeded, which is what matters most
        }

        return savedConfig;
    }

    /**
     * Updates an existing product configuration in both MongoDB and Redis.
     * MongoDB update is performed first, as it's the source of truth.
     */
    @Transactional
    public ProductConfig updateProductConfig(String id, ProductConfig config) {
        logger.info("Updating product configuration with ID: {}", id);

        // Ensure the ID is set on the configuration
        config.setId(id);

        // Update MongoDB first (source of truth)
        ProductConfig updatedConfig = mongoRepository.save(config);

        try {
            // Then update Redis
            ProductConfigRedis redisConfig = ProductConfigRedis.fromProductConfig(updatedConfig);
            redisRepository.save(redisConfig);
            logger.debug("Successfully updated product configuration in both MongoDB and Redis");
        } catch (Exception e) {
            logger.error("Failed to update Redis, but MongoDB update succeeded: {}", e.getMessage());
            // MongoDB update succeeded, which is what matters most
        }

        return updatedConfig;
    }

    /**
     * Deletes a product configuration from both MongoDB and Redis.
     * MongoDB deletion is performed first, as it's the source of truth.
     */
    @Transactional
    public void deleteProductConfig(String id) {
        logger.info("Deleting product configuration with ID: {}", id);

        try {
            // Delete from MongoDB first (source of truth)
            mongoRepository.deleteById(id);

            // Then delete from Redis
            redisRepository.deleteById(id);
            logger.debug("Successfully deleted product configuration from both MongoDB and Redis");
        } catch (Exception e) {
            logger.error("Error during deletion process: {}", e.getMessage());
            // Attempt to clean up Redis regardless of MongoDB outcome
            try {
                redisRepository.deleteById(id);
            } catch (Exception redisError) {
                logger.error("Failed to delete from Redis: {}", redisError.getMessage());
            }
        }
    }

    /**
     * Retrieves all product configurations from MongoDB and syncs them to Redis.
     * Use this method to restore Redis data from MongoDB (source of truth).
     */
    public int syncFromMongoToRedis() {
        logger.info("Starting full synchronization from MongoDB to Redis");
        List<ProductConfig> mongoConfigs = mongoRepository.findAll();
        int syncCount = 0;

        // Clear Redis first to avoid stale data
        try {
            redisRepository.deleteAll();
            logger.info("Cleared existing Redis data before sync");
        } catch (Exception e) {
            logger.error("Failed to clear Redis before sync: {}", e.getMessage());
        }

        // Sync all configurations from MongoDB to Redis
        for (ProductConfig mongoConfig : mongoConfigs) {
            try {
                ProductConfigRedis redisConfig = ProductConfigRedis.fromProductConfig(mongoConfig);
                redisRepository.save(redisConfig);
                syncCount++;
            } catch (Exception e) {
                logger.error("Failed to sync config ID {} to Redis: {}", mongoConfig.getId(), e.getMessage());
            }
        }

        logger.info("Synchronized {} product configurations from MongoDB to Redis", syncCount);
        return syncCount;
    }

    /**
     * Retrieves the list of distinct active product IDs from Redis.
     * If Redis is unavailable, falls back to MongoDB.
     */
    public List<String> getDistinctActiveProductIds() {
        LocalDateTime now = LocalDateTime.now();

        try {
            // First try to get data from Redis
            List<String> activeProductIds = redisRepository.findActiveConfigurations(now).stream()
                    .map(ProductConfigRedis::getProductId)
                    .distinct()
                    .collect(Collectors.toList());

            if (!activeProductIds.isEmpty()) {
                logger.debug("Retrieved {} active product IDs from Redis", activeProductIds.size());
                return activeProductIds;
            }

            logger.warn("No active product IDs found in Redis, falling back to MongoDB");
        } catch (Exception e) {
            logger.error("Redis error, falling back to MongoDB: {}", e.getMessage());
        }

        // Fallback to MongoDB
        List<String> activeProductIds = mongoRepository.findAll().stream()
                .filter(config -> config.isEnabled() &&
                        config.getStartDate().isBefore(now) &&
                        config.getEndDate().isAfter(now))
                .map(ProductConfig::getProductId)
                .distinct()
                .collect(Collectors.toList());

        logger.debug("Retrieved {} active product IDs from MongoDB (fallback)", activeProductIds.size());
        return activeProductIds;
    }

    /**
     * Gets a product configuration by ID, preferring Redis but falling back to
     * MongoDB.
     */
    public Optional<ProductConfig> getProductConfigById(String id) {
        try {
            // First try Redis
            Optional<ProductConfigRedis> redisConfig = redisRepository.findById(id);

            if (redisConfig.isPresent()) {
                // Convert Redis config to MongoDB model for consistent API
                ProductConfigRedis config = redisConfig.get();
                ProductConfig result = new ProductConfig();
                result.setId(config.getId());
                result.setProductId(config.getProductId());
                result.setEnabled(config.isEnabled());
                result.setStartDate(config.getStartDate());
                result.setEndDate(config.getEndDate());

                logger.debug("Retrieved product configuration from Redis");
                return Optional.of(result);
            }
        } catch (Exception e) {
            logger.error("Redis error, falling back to MongoDB: {}", e.getMessage());
        }

        // Fallback to MongoDB
        Optional<ProductConfig> mongoConfig = mongoRepository.findById(id);
        if (mongoConfig.isPresent()) {
            logger.debug("Retrieved product configuration from MongoDB (fallback)");
        }

        return mongoConfig;
    }

    /**
     * Scheduled job to sync MongoDB data to Redis.
     * Runs every hour by default.
     */
    @Scheduled(fixedRateString = "${redis.sync.interval:3600000}")
    public void scheduledSync() {
        logger.info("Running scheduled sync from MongoDB to Redis");
        try {
            int count = syncFromMongoToRedis();
            logger.info("Scheduled sync completed, synchronized {} configurations", count);
        } catch (Exception e) {
            logger.error("Scheduled sync failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifies consistency between MongoDB and Redis.
     * Returns true if all configurations match, false otherwise.
     */
    public boolean verifyConsistency() {
        logger.info("Verifying consistency between MongoDB and Redis");

        List<ProductConfig> mongoConfigs = mongoRepository.findAll();
        List<ProductConfigRedis> redisConfigs = StreamSupport
                .stream(redisRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());

        if (mongoConfigs.size() != redisConfigs.size()) {
            logger.warn("Consistency check failed: MongoDB has {} configs, Redis has {}",
                    mongoConfigs.size(), redisConfigs.size());
            return false;
        }

        for (ProductConfig mongoConfig : mongoConfigs) {
            Optional<ProductConfigRedis> redisConfig = redisRepository.findById(mongoConfig.getId());

            if (redisConfig.isEmpty()) {
                logger.warn("Consistency check failed: Config ID {} exists in MongoDB but not in Redis",
                        mongoConfig.getId());
                return false;
            }

            ProductConfigRedis redisConfigData = redisConfig.get();
            if (!mongoConfig.getProductId().equals(redisConfigData.getProductId()) ||
                    mongoConfig.isEnabled() != redisConfigData.isEnabled()) {
                logger.warn("Consistency check failed: Config ID {} has mismatched data between MongoDB and Redis",
                        mongoConfig.getId());
                return false;
            }
        }

        logger.info("Consistency check passed: MongoDB and Redis are in sync");
        return true;
    }
}