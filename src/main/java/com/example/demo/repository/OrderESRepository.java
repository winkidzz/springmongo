package com.example.demo.repository;

import com.example.demo.model.OrderES;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderESRepository extends ElasticsearchRepository<OrderES, String> {
    List<OrderES> findByStatus(String status);

    List<OrderES> findByStatusAndCreatedAtAfter(String status, LocalDateTime startDate);

    List<OrderES> findByStatusAndCreatedAtBetween(String status, LocalDateTime startDate, LocalDateTime endDate);
}