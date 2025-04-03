package com.example.demo.repository;

import com.example.demo.model.ProductConfigRedis;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductConfigRedisRepository extends CrudRepository<ProductConfigRedis, String> {

    // Find all products that are enabled
    List<ProductConfigRedis> findByEnabledTrue();

    // Find products by product ID
    List<ProductConfigRedis> findByProductId(String productId);

    // Find all enabled configurations for a list of product IDs
    List<ProductConfigRedis> findByProductIdInAndEnabledTrue(List<String> productIds);

    // Delete all configurations for a specific product
    void deleteByProductId(String productId);

    // Custom finder for active products (implementation will be provided by Spring
    // Data Redis)
    default List<ProductConfigRedis> findActiveConfigurations(LocalDateTime now) {
        return findAll().stream()
                .filter(config -> config.isEnabled() &&
                        config.getStartDate().isBefore(now) &&
                        config.getEndDate().isAfter(now))
                .toList();
    }
}