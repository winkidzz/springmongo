package com.example.demo.repository;

import com.example.demo.model.Order;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepositoryCustom {
    List<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}