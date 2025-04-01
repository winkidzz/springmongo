package com.example.demo.controller;

import com.example.demo.util.DataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data")
public class DataGeneratorController {
    private final DataGenerator dataGenerator;

    @Autowired
    public DataGeneratorController(DataGenerator dataGenerator) {
        this.dataGenerator = dataGenerator;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateData(@RequestParam(defaultValue = "1000") int numOrders) {
        dataGenerator.generateTestData(numOrders);
        return ResponseEntity.ok("Generated " + numOrders + " test orders");
    }
}