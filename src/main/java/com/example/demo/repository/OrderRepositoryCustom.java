package com.example.demo.repository;

import com.example.demo.dto.OrderSummaryDTO;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepositoryCustom {
    List<OrderSummaryDTO> findOrderSummariesWithMetrics(LocalDateTime startDate, LocalDateTime endDate);
}