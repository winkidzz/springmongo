package com.example.demo.controller;

import com.example.demo.service.PerformanceTestDataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/performance-test")
public class PerformanceTestController {

    @Autowired
    private PerformanceTestDataGenerator dataGenerator;

    @PostMapping("/generate")
    public ResponseEntity<String> generateTestData(
            @RequestParam(defaultValue = "100000") int numOrders,
            @RequestParam(defaultValue = "30") int numConfigs) {
        try {
            dataGenerator.generateTestData(numOrders, numConfigs);
            return ResponseEntity.ok("Test data generation completed successfully");
        } catch (Exception e) {
            log.error("Error generating test data", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getStatistics() {
        try {
            dataGenerator.displayStatistics();
            return ResponseEntity.ok("Statistics displayed in logs");
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearTestData() {
        try {
            dataGenerator.clearTestData();
            return ResponseEntity.ok("Test data cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing test data", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}