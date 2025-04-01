package com.example.demo.repository;

import com.example.demo.model.ProductConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductConfigRepository extends MongoRepository<ProductConfig, String> {
}