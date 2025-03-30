package com.example.demo.controller;

import com.example.demo.model.ProductConfig;
import com.example.demo.repository.ProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/product-configs")
public class ProductConfigController {

    @Autowired
    private ProductConfigRepository productConfigRepository;

    @GetMapping
    public ResponseEntity<List<ProductConfig>> getAllConfigs() {
        return ResponseEntity.ok(productConfigRepository.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ProductConfig>> getActiveConfigs() {
        return ResponseEntity.ok(productConfigRepository.findActiveConfigs(LocalDateTime.now()));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<List<ProductConfig>> getConfigsByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(productConfigRepository.findByProductId(productId));
    }
} 