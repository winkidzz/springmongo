package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("/collections")
    public Map<String, Object> getCollectionStats() {
        Map<String, Object> stats = new HashMap<>();

        // Get all collection names
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            long count = mongoTemplate.getCollection(collectionName).countDocuments();
            stats.put(collectionName, count);
        }

        return stats;
    }
}