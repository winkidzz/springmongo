package com.example.demo.repository;

import com.example.demo.model.ProductConfigES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductConfigESRepository extends ElasticsearchRepository<ProductConfigES, String> {
    List<ProductConfigES> findByEnabledTrue();

    List<ProductConfigES> findByEnabledTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDateTime now, LocalDateTime now2);

    List<ProductConfigES> findByProductIdInAndEnabledTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            List<String> productIds, LocalDateTime now, LocalDateTime now2);
}