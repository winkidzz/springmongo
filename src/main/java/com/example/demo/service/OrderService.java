package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order createOrder(Order order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setStatus("PENDING");
        return orderRepository.save(order);
    }

    public List<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findOrdersByDateRange(startDate, endDate);
    }

    public List<Order> findByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    public List<Order> findByStatusAndCreatedAfter(String status, LocalDateTime startDate) {
        return orderRepository.findByStatusAndCreatedAfter(status, startDate);
    }

    public List<Order> findByStatusAndCreatedBetween(String status, LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByStatusAndCreatedBetween(status, startDate, endDate);
    }

    public List<Order> findByStatusAndCreatedBetweenAndAmountGreaterThan(String status, LocalDateTime startDate,
            LocalDateTime endDate, double amount) {
        return orderRepository.findByStatusAndCreatedBetweenAndAmountGreaterThan(status, startDate, endDate, amount);
    }
}