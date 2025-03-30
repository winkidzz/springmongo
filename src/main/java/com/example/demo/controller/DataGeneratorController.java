package com.example.demo.controller;

import com.example.demo.util.DataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data")
public class DataGeneratorController {

    @Autowired
    private DataGenerator dataGenerator;

    @PostMapping("/generate")
    public ResponseEntity<String> generateData() {
        dataGenerator.generateData();
        return ResponseEntity.ok("Data generation completed successfully");
    }
} 