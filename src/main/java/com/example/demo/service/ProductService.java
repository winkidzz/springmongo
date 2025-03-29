package com.example.demo.service;

import com.example.demo.model.ProductConfig;
import com.example.demo.repository.ProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductConfigRepository productConfigRepository;

    public List<ProductConfig> getActiveProducts() {
        return productConfigRepository.findActiveConfigs(LocalDateTime.now());
    }
}