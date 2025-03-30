package com.example.demo.service;

import com.example.demo.dto.OrderSummaryDTO;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public List<OrderSummaryDTO> getOrderSummaries() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(10);
        LocalDateTime endDate = now.plusDays(10);

        return orderRepository.findOrderSummariesWithMetrics(startDate, endDate);
    }
}