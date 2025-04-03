package com.example.demo.repository;

import java.util.List;

public interface OrderESRepositoryCustom {
    List<String> findDistinctActiveProductsES();

    List<String> findDistinctActiveProductsESNative();

    /**
     * High-performance implementation for finding active products
     * 
     * @return List of active product IDs
     */
    List<String> findDistinctActiveProductsOptimized();
}