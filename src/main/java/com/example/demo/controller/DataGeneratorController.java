package com.example.demo.controller;

import com.example.demo.service.DataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data")
public class DataGeneratorController {
    @Autowired
    private DataGenerator dataGenerator;

    @PostMapping("/generate")
    public String generateTestData(@RequestParam(defaultValue = "1000") int numOrders) {
        dataGenerator.generateTestData(numOrders);
        return "Generated " + numOrders + " test orders";
    }
}