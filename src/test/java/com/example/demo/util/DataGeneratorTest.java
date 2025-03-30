package com.example.demo.util;

import com.example.demo.controller.ProductController;
import com.example.demo.model.ProductConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
public class DataGeneratorTest {

    @Autowired
    private DataGenerator dataGenerator;

    @Test
    public void testSmallDataSetGeneration() {
        // Clear existing data
        ProductController.products.clear();
        
        // Generate data
        dataGenerator.generateData();
        
        // Verify total product count
        assertEquals(20000, ProductController.products.size(), "Should generate 20k products");
        
        verifyProductProperties();
        verifyProductDistribution();
    }
    
    @Test
    public void testLargeDataSetGeneration() {
        // Clear existing data
        ProductController.products.clear();
        
        // Generate large dataset
        dataGenerator.generateLargeDataSet();
        
        // Verify total product count
        assertEquals(200000, ProductController.products.size(), "Should generate 200k products");
        
        verifyProductProperties();
        verifyProductDistribution();
    }
    
    private void verifyProductProperties() {
        ProductController.products.values().forEach(product -> {
            assertNotNull(product.getId(), "Product ID should not be null");
            assertNotNull(product.getName(), "Product name should not be null");
            assertTrue(product.getName().startsWith("Product-"), "Product name should start with 'Product-'");
            assertTrue(product.getPrice() >= 0 && product.getPrice() <= 1000, "Product price should be between 0 and 1000");
            assertNotNull(product.getDescription(), "Product description should not be null");
        });
    }
    
    private void verifyProductDistribution() {
        // Verify product distribution across different config types
        Map<String, Long> configTypeCounts = ProductController.products.values().stream()
            .collect(Collectors.groupingBy(
                product -> product.getDescription().split(" ")[2], // Extract config type
                Collectors.counting()
            ));
        
        // Calculate expected counts based on total products
        int totalProducts = ProductController.products.size();
        int productsPerConfig = totalProducts / 20; // 20 total configs
        
        // Verify counts for each config type
        assertEquals(productsPerConfig * 5, configTypeCounts.get("Currently"), "Should have correct number of currently active products");
        assertEquals(productsPerConfig * 3, configTypeCounts.get("Future"), "Should have correct number of future products");
        assertEquals(productsPerConfig * 3, configTypeCounts.get("Past"), "Should have correct number of past products");
        assertEquals(productsPerConfig * 3, configTypeCounts.get("Disabled"), "Should have correct number of disabled products");
        assertEquals(productsPerConfig * 3, configTypeCounts.get("Long"), "Should have correct number of long-term products");
        assertEquals(productsPerConfig * 3, configTypeCounts.get("Short"), "Should have correct number of short-term products");
        
        // Verify product descriptions contain correct config types
        assertTrue(ProductController.products.values().stream()
            .anyMatch(p -> p.getDescription().contains("Currently Active")), 
            "Should have products with Currently Active configs");
        assertTrue(ProductController.products.values().stream()
            .anyMatch(p -> p.getDescription().contains("Future")), 
            "Should have products with Future configs");
        assertTrue(ProductController.products.values().stream()
            .anyMatch(p -> p.getDescription().contains("Past")), 
            "Should have products with Past configs");
        assertTrue(ProductController.products.values().stream()
            .anyMatch(p -> p.getDescription().contains("Disabled")), 
            "Should have products with Disabled configs");
        assertTrue(ProductController.products.values().stream()
            .anyMatch(p -> p.getDescription().contains("Long Term")), 
            "Should have products with Long Term configs");
        assertTrue(ProductController.products.values().stream()
            .anyMatch(p -> p.getDescription().contains("Short Term")), 
            "Should have products with Short Term configs");
    }
} 