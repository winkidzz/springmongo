package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.util.DataGenerator;

@RestController
@RequestMapping("/api/data")
public class DataGeneratorController {
    private final DataGenerator dataGenerator;

    public DataGeneratorController(DataGenerator dataGenerator) {
        this.dataGenerator = dataGenerator;
    }

    @PostMapping("/generate")
    public String generateData() {
        try {
            long startTime = System.currentTimeMillis();
            dataGenerator.generateTestData();
            long endTime = System.currentTimeMillis();
            return "Data generation completed successfully in " + (endTime - startTime) / 1000 + " seconds";
        } catch (Exception e) {
            return "Error generating data: " + e.getMessage();
        }
    }
}