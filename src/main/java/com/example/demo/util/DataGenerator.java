package com.example.demo.util;

import com.example.demo.model.ProductConfig;
import com.example.demo.repository.ProductConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DataGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(DataGenerator.class);
    
    @Autowired
    private ProductConfigRepository productConfigRepository;
    
    public void generateData() {
        logger.info("Starting test data generation...");
        
        // Clear existing data
        productConfigRepository.deleteAll();
        logger.info("Cleared existing configurations");
        
        // Generate two test configurations
        createTestConfig("test-config-1", true);
        createTestConfig("test-config-2", false);
        
        logger.info("Finished generating test configurations");
    }
    
    private void createTestConfig(String id, boolean enabled) {
        LocalDateTime now = LocalDateTime.now();
        ProductConfig config = new ProductConfig();
        config.setId(id);
        config.setProductId(id);
        config.setConfigName("Test Config " + id);
        config.setConfigValue("Value " + id);
        config.setStartDate(now.minusDays(30));
        config.setEndDate(now.plusDays(30));
        config.setReleaseStartDate(now.minusDays(30));
        config.setReleaseEndDate(now.plusDays(30));
        config.setCreatedStartDate(now.minusDays(30));
        config.setCreatedEndDate(now.plusDays(30));
        config.setEnabled(enabled);
        
        productConfigRepository.save(config);
        logger.info("Created test configuration: {}", id);
    }
} 