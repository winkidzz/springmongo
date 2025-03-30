package com.example.demo.util;

import com.example.demo.controller.ProductController;
import com.example.demo.model.ProductConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DataGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(DataGenerator.class);
    private static final int SMALL_BATCH_SIZE = 1000;  // For regular tests
    private static final int LARGE_BATCH_SIZE = 33333; // For 200k products (6 configs * 33333)
    
    public void generateData() {
        generateData(SMALL_BATCH_SIZE);
    }
    
    public void generateLargeDataSet() {
        generateData(LARGE_BATCH_SIZE);
    }
    
    private void generateData(int productsPerConfig) {
        logger.info("Starting comprehensive data generation with {} products per config...", productsPerConfig);
        
        // Clear existing data
        ProductController.products.clear();
        logger.info("Cleared existing products");
        
        // Generate different types of configurations and products
        generateCurrentlyActiveConfigs(productsPerConfig);
        generateFutureConfigs(productsPerConfig);
        generatePastConfigs(productsPerConfig);
        generateDisabledConfigs(productsPerConfig);
        generateLongTermConfigs(productsPerConfig);
        generateShortTermConfigs(productsPerConfig);
        
        logger.info("Finished generating products. Total products: {}", ProductController.products.size());
    }
    
    private void generateCurrentlyActiveConfigs(int productsPerConfig) {
        logger.info("Generating currently active configurations...");
        LocalDateTime now = LocalDateTime.now();
        
        // Generate 5 configurations that are currently active
        for (int i = 0; i < 5; i++) {
            ProductConfig config = createConfig(
                "active-" + i,
                now.minusDays(30),
                now.plusDays(30),
                true,
                now.minusDays(30),
                now.plusDays(30)
            );
            generateProductsForConfig(config, "Currently Active", productsPerConfig);
        }
    }
    
    private void generateFutureConfigs(int productsPerConfig) {
        logger.info("Generating future configurations...");
        LocalDateTime now = LocalDateTime.now();
        
        // Generate 3 configurations that will be active in the future
        for (int i = 0; i < 3; i++) {
            ProductConfig config = createConfig(
                "future-" + i,
                now.plusDays(30),
                now.plusDays(60),
                true,
                now.plusDays(30),
                now.plusDays(60)
            );
            generateProductsForConfig(config, "Future", productsPerConfig);
        }
    }
    
    private void generatePastConfigs(int productsPerConfig) {
        logger.info("Generating past configurations...");
        LocalDateTime now = LocalDateTime.now();
        
        // Generate 3 configurations that were active in the past
        for (int i = 0; i < 3; i++) {
            ProductConfig config = createConfig(
                "past-" + i,
                now.minusDays(60),
                now.minusDays(30),
                false,
                now.minusDays(60),
                now.minusDays(30)
            );
            generateProductsForConfig(config, "Past", productsPerConfig);
        }
    }
    
    private void generateDisabledConfigs(int productsPerConfig) {
        logger.info("Generating disabled configurations...");
        LocalDateTime now = LocalDateTime.now();
        
        // Generate 3 configurations that are disabled
        for (int i = 0; i < 3; i++) {
            ProductConfig config = createConfig(
                "disabled-" + i,
                now.minusDays(30),
                now.plusDays(30),
                false,
                now.minusDays(30),
                now.plusDays(30)
            );
            generateProductsForConfig(config, "Disabled", productsPerConfig);
        }
    }
    
    private void generateLongTermConfigs(int productsPerConfig) {
        logger.info("Generating long-term configurations...");
        LocalDateTime now = LocalDateTime.now();
        
        // Generate 3 configurations that are active for a long period
        for (int i = 0; i < 3; i++) {
            ProductConfig config = createConfig(
                "long-term-" + i,
                now.minusDays(365),
                now.plusDays(365),
                true,
                now.minusDays(365),
                now.plusDays(365)
            );
            generateProductsForConfig(config, "Long Term", productsPerConfig);
        }
    }
    
    private void generateShortTermConfigs(int productsPerConfig) {
        logger.info("Generating short-term configurations...");
        LocalDateTime now = LocalDateTime.now();
        
        // Generate 3 configurations that are active for a short period
        for (int i = 0; i < 3; i++) {
            ProductConfig config = createConfig(
                "short-term-" + i,
                now.minusDays(7),
                now.plusDays(7),
                true,
                now.minusDays(7),
                now.plusDays(7)
            );
            generateProductsForConfig(config, "Short Term", productsPerConfig);
        }
    }
    
    private ProductConfig createConfig(String id, LocalDateTime startDate, LocalDateTime endDate,
                                     boolean enabled, LocalDateTime createdStart, LocalDateTime createdEnd) {
        ProductConfig config = new ProductConfig();
        config.setId(id);
        config.setProductId(id);
        config.setConfigName("Config-" + id);
        config.setConfigValue("Value-" + id);
        config.setStartDate(startDate);
        config.setEndDate(endDate);
        config.setReleaseStartDate(startDate);
        config.setReleaseEndDate(endDate);
        config.setCreatedStartDate(createdStart);
        config.setCreatedEndDate(createdEnd);
        config.setEnabled(enabled);
        return config;
    }
    
    private void generateProductsForConfig(ProductConfig config, String configType, int productsPerConfig) {
        logger.info("Generating {} products for {} configuration {}", 
                   productsPerConfig, configType, config.getId());
        
        for (int i = 0; i < productsPerConfig; i++) {
            String id = UUID.randomUUID().toString();
            if (id != null) {
                ProductController.Product product = new ProductController.Product();
                product.setId(id);
                product.setName("Product-" + id.substring(0, 8));
                product.setPrice(Math.random() * 1000);
                product.setDescription(String.format("Product generated from %s config %s", 
                                                   configType, config.getId()));
                ProductController.products.put(id, product);
                
                if (i % 10000 == 0) { // Log progress every 10k products
                    logger.info("Generated {} products for {} config {}", 
                              i, configType, config.getId());
                }
            }
        }
    }
} 