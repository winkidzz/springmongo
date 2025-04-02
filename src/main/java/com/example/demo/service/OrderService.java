package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.OrderRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderRepositoryCustom orderRepositoryCustom;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderRepositoryCustom orderRepositoryCustom,
            MongoTemplate mongoTemplate) {
        this.orderRepository = orderRepository;
        this.orderRepositoryCustom = orderRepositoryCustom;
        this.mongoTemplate = mongoTemplate;
    }

    public Order createOrder(Order order) {
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setStatus("PENDING");
        return orderRepository.save(order);
    }

    public List<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepositoryCustom.findOrdersByDateRange(startDate, endDate);
    }

    public List<Order> findByStatus(String status) {
        Query query = new Query(Criteria.where("status").is(status));
        return mongoTemplate.find(query, Order.class);
    }

    public List<Order> findByStatusAndCreatedAfter(String status, LocalDateTime startDate) {
        return orderRepository.findByStatusAndCreatedAfter(status, startDate);
    }

    public List<Order> findByStatusAndCreatedBetween(String status, LocalDateTime startDate, LocalDateTime endDate) {
        Query query = new Query(Criteria.where("status").is(status)
                .and("createdAt").gte(startDate).lte(endDate));
        return mongoTemplate.find(query, Order.class);
    }

    public List<Order> findByStatusAndCreatedBetweenAndAmountGreaterThan(String status, LocalDateTime startDate,
            LocalDateTime endDate, double minAmount) {
        Query query = new Query(Criteria.where("status").is(status)
                .and("createdAt").gte(startDate).lte(endDate)
                .and("amount").gt(minAmount));
        return mongoTemplate.find(query, Order.class);
    }

    public List<String> findDistinctActiveProducts() {
        return orderRepositoryCustom.findDistinctActiveProducts();
    }

    public List<String> findDistinctActiveProductsOptimized() {
        return ((com.example.demo.repository.OrderRepositoryImpl) orderRepositoryCustom)
                .findDistinctActiveProductsOptimized();
    }

    public List<String> findDistinctActiveProductsWithMongoDistinct() {
        return ((com.example.demo.repository.OrderRepositoryImpl) orderRepositoryCustom)
                .findDistinctActiveProductsWithMongoDistinct();
    }
}