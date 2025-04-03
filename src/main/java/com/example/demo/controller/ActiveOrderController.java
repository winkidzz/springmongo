package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.service.MongoDbOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for accessing orders for active products.
 * Uses Redis for fast active product ID lookup, then retrieves orders from
 * MongoDB.
 */
@RestController
@RequestMapping("/api/orders")
public class ActiveOrderController {
    private static final Logger logger = LoggerFactory.getLogger(ActiveOrderController.class);

    private final MongoDbOrderService orderService;

    @Autowired
    public ActiveOrderController(MongoDbOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Returns all orders for active products.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Order>> getOrdersForActiveProducts() {
        long startTime = System.currentTimeMillis();
        logger.info("API request for orders of active products");

        List<Order> orders = orderService.getOrdersForActiveProducts();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Returned {} orders for active products in {} ms", orders.size(), duration);

        return ResponseEntity.ok(orders);
    }

    /**
     * Returns all active products that have orders.
     */
    @GetMapping("/active-products")
    public ResponseEntity<List<String>> getActiveProductsWithOrders() {
        long startTime = System.currentTimeMillis();
        logger.info("API request for active products with orders");

        List<String> productIds = orderService.getActiveProductsWithOrders();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Returned {} active products with orders in {} ms", productIds.size(), duration);

        return ResponseEntity.ok(productIds);
    }

    /**
     * Returns orders for a specific product ID if it is active.
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Order>> getOrdersForProduct(@PathVariable String productId) {
        long startTime = System.currentTimeMillis();
        logger.info("API request for orders of product ID: {}", productId);

        List<Order> orders = orderService.getOrdersForProduct(productId);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Returned {} orders for product ID {} in {} ms", orders.size(), productId, duration);

        return ResponseEntity.ok(orders);
    }

    /**
     * Returns counts for a given active product.
     */
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getProductOrderStats(@PathVariable String productId) {
        long startTime = System.currentTimeMillis();
        logger.info("API request for order stats of product ID: {}", productId);

        List<Order> orders = orderService.getOrdersForProduct(productId);

        // Only process if the product is active (otherwise, orders will be empty)
        if (orders.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "productId", productId,
                    "active", false,
                    "orderCount", 0));
        }

        // Calculate some basic stats
        double totalAmount = orders.stream()
                .mapToDouble(Order::getAmount)
                .sum();

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Calculated stats for product ID {} in {} ms", productId, duration);

        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "active", true,
                "orderCount", orders.size(),
                "totalAmount", totalAmount));
    }
}