package com.example.demo.util;

import com.example.demo.model.ProductConfig;
import com.example.demo.repository.ProductConfigRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@SpringBootTest
public class DataGeneratorTest {

    @Autowired
    private DataGenerator dataGenerator;

    @Autowired
    private ProductConfigRepository productConfigRepository;

    @Test
    public void testDataGeneration() {
        // Generate data
        dataGenerator.generateData();
        
        // Verify configurations
        List<ProductConfig> configs = productConfigRepository.findAll();
        assertEquals(2, configs.size(), "Should generate 2 test configurations");
        
        // Verify first config
        ProductConfig config1 = configs.stream()
            .filter(c -> c.getId().equals("test-config-1"))
            .findFirst()
            .orElse(null);
        assertNotNull(config1, "First config should exist");
        assertTrue(config1.isEnabled(), "First config should be enabled");
        assertEquals("Test Config test-config-1", config1.getConfigName());
        assertEquals("Value test-config-1", config1.getConfigValue());
        
        // Verify second config
        ProductConfig config2 = configs.stream()
            .filter(c -> c.getId().equals("test-config-2"))
            .findFirst()
            .orElse(null);
        assertNotNull(config2, "Second config should exist");
        assertFalse(config2.isEnabled(), "Second config should be disabled");
        assertEquals("Test Config test-config-2", config2.getConfigName());
        assertEquals("Value test-config-2", config2.getConfigValue());
    }
} 