package com.example.demo.controller;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    public static final Map<String, Product> products = new ConcurrentHashMap<>();

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        if (product == null || product.getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        products.put(product.getId(), product);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }
        Product product = products.get(id);
        return product != null ? ResponseEntity.ok(product) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> productList = products.values().stream()
            .filter(product -> product != null)
            .collect(Collectors.toList());
        return ResponseEntity.ok(productList);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Product>> getActiveProducts() {
        LocalDateTime now = LocalDateTime.now();
        
        // Create aggregation pipeline to find products with active configurations
        Aggregation aggregation = Aggregation.newAggregation(
            // Match active configurations
            Aggregation.match(Criteria.where("enabled").is(true)
                .and("startDate").lte(now)
                .and("endDate").gte(now)),
            // Group by productId
            Aggregation.group("productId")
        );
        
        // Execute aggregation to get list of active product IDs
        AggregationResults<Document> results = mongoTemplate.aggregate(
            aggregation, "product_configs", Document.class);
        
        // Extract product IDs and fetch corresponding products
        List<Product> activeProducts = results.getMappedResults().stream()
            .map(doc -> doc.get("_id").toString()) // _id contains the productId after grouping
            .map(products::get)
            .filter(product -> product != null)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(activeProducts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        if (id == null || !products.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        products.put(id, product);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (id == null || !products.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        products.remove(id);
        return ResponseEntity.ok().build();
    }

    public static class Product {
        private String id;
        private String name;
        private double price;
        private String description;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}