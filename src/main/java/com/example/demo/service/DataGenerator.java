package com.example.demo.service;

import com.example.demo.util.TestDataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataGenerator {
    @Autowired
    private TestDataGenerator testDataGenerator;

    public void generateTestData(int numOrders) {
        testDataGenerator.generateTestData(numOrders);
    }
}