package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
        List<Order> findByStatus(String status);

        @Query(value = "{ 'status': ?0, 'createdAt': { $gte: ?1 } }")
        List<Order> findByStatusAndCreatedAfter(String status, LocalDateTime startDate);

        @Query(value = "{ 'status': ?0, 'createdAt': { $gte: ?1, $lte: ?2 } }")
        List<Order> findByStatusAndCreatedBetween(String status, LocalDateTime startDate, LocalDateTime endDate);

        @Query(value = "{ 'status': ?0, 'createdAt': { $gte: ?1, $lte: ?2 }, 'amount': { $gte: ?3 } }")
        List<Order> findByStatusAndCreatedBetweenAndAmountGreaterThan(String status, LocalDateTime startDate,
                        LocalDateTime endDate, double amount);

        @Query(value = "{ 'status': ?0, 'createdAt': { $gte: ?1, $lte: ?2 }, 'amount': { $gte: ?3 } }", fields = "{ 'id': 1, 'amount': 1, 'createdAt': 1, 'status': 1 }")
        List<Order> findOrderSummariesByStatusAndCreatedBetweenAndAmountGreaterThan(String status,
                        LocalDateTime startDate, LocalDateTime endDate, double amount);

        @Query(value = "{ 'status': ?0, 'createdAt': { $gte: ?1, $lte: ?2 }, 'amount': { $gte: ?3 } }", fields = "{ 'id': 1, 'amount': 1, 'createdAt': 1, 'status': 1, 'productId': 1 }")
        List<Order> findOrderSummariesWithProductIdByStatusAndCreatedBetweenAndAmountGreaterThan(String status,
                        LocalDateTime startDate, LocalDateTime endDate, double amount);

        @Query(value = "{ 'status': ?0, 'createdAt': { $gte: ?1, $lte: ?2 }, 'amount': { $gte: ?3 } }", fields = "{ 'id': 1, 'amount': 1, 'createdAt': 1, 'status': 1, 'productId': 1, 'customerId': 1 }")
        List<Order> findOrderSummariesWithCustomerIdByStatusAndCreatedBetweenAndAmountGreaterThan(String status,
                        LocalDateTime startDate, LocalDateTime endDate, double amount);
}