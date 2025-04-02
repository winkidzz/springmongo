package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
        private final OrderService orderService;

        @Autowired
        public OrderController(OrderService orderService) {
                this.orderService = orderService;
        }

        @PostMapping
        public ResponseEntity<Order> createOrder(@RequestBody Order order) {
                return ResponseEntity.ok(orderService.createOrder(order));
        }

        @GetMapping("/by-date-range")
        public ResponseEntity<List<Order>> getOrdersByDateRange(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
                return ResponseEntity.ok(orderService.findOrdersByDateRange(startDate, endDate));
        }

        @GetMapping("/by-status")
        public ResponseEntity<List<Order>> getOrdersByStatus(@RequestParam String status) {
                return ResponseEntity.ok(orderService.findByStatus(status));
        }

        @GetMapping("/by-status-and-date")
        public ResponseEntity<List<Order>> getOrdersByStatusAndDate(
                        @RequestParam String status,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
                return ResponseEntity.ok(orderService.findByStatusAndCreatedBetween(status, startDate, endDate));
        }

        @GetMapping("/by-status-and-amount")
        public ResponseEntity<List<Order>> getOrdersByStatusAndAmount(
                        @RequestParam String status,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                        @RequestParam double minAmount) {
                return ResponseEntity.ok(orderService.findByStatusAndCreatedBetweenAndAmountGreaterThan(status,
                                startDate, endDate, minAmount));
        }

        @GetMapping("/active-products")
        public ResponseEntity<List<String>> getActiveProducts() {
                return ResponseEntity.ok(orderService.findDistinctActiveProducts());
        }

        @GetMapping("/active-products-original")
        public ResponseEntity<List<String>> getActiveProductsUsingAggregation() {
                return ResponseEntity.ok(orderService.findDistinctActiveProductsWithAggregation());
        }

        @GetMapping("/active-products-optimized")
        public ResponseEntity<List<String>> getActiveProductsOptimized() {
                return ResponseEntity.ok(orderService.findDistinctActiveProductsOptimized());
        }
}