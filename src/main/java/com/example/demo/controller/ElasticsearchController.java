package com.example.demo.controller;

import com.example.demo.service.ElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elasticsearch")
public class ElasticsearchController {

    @Autowired
    private ElasticsearchService elasticsearchService;

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncData() {
        elasticsearchService.syncAllDataToElasticsearch();
        return ResponseEntity.ok(Map.of("message", "Data synchronized to Elasticsearch"));
    }

    @GetMapping("/active-products")
    public ResponseEntity<List<String>> getActiveProductsES() {
        return ResponseEntity.ok(elasticsearchService.findDistinctActiveProductsES());
    }

    @GetMapping("/active-products-native")
    public ResponseEntity<List<String>> getActiveProductsESNative() {
        return ResponseEntity.ok(elasticsearchService.findDistinctActiveProductsESNative());
    }

    @PostMapping("/generate-test-data")
    public ResponseEntity<Map<String, String>> generateTestData(
            @RequestParam(defaultValue = "100") int numProducts,
            @RequestParam(defaultValue = "100000") int numOrders) {
        elasticsearchService.generateLargeTestData(numProducts, numOrders);
        return ResponseEntity.ok(Map.of(
                "message", "Generated " + numOrders + " orders and " + numProducts + " product configs"));
    }
}