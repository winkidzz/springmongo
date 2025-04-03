package com.example.demo.repository;

import com.example.demo.model.ProductConfigES;
import java.time.LocalDateTime;
import java.util.List;

public interface ProductConfigESRepositoryCustom {
    List<ProductConfigES> findActiveProductConfigsByProductIds(List<String> productIds, LocalDateTime now);
}