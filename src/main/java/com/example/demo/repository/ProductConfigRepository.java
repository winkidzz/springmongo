package com.example.demo.repository;

import com.example.demo.model.ProductConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductConfigRepository extends MongoRepository<ProductConfig, String> {
    @Query("{ 'enabled': true, 'startDate': { $lte: ?0 }, 'endDate': { $gte: ?0 } }")
    List<ProductConfig> findActiveConfigs(LocalDateTime currentDate);

    List<ProductConfig> findByProductId(String productId);

    long countByEnabled(boolean enabled);
}