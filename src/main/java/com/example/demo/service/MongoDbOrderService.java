package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving orders from MongoDB based on active product
 * configurations.
 * Uses the dual-write service for fast access to active product IDs.
 */
@Service
public class MongoDbOrderService {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbOrderService.class);

    private final OrderRepository orderRepository;
    private final ProductConfigDualWriteService dualWriteService;

    @Autowired
    public MongoDbOrderService(
            OrderRepository orderRepository,
            ProductConfigDualWriteService dualWriteService) {
        this.orderRepository = orderRepository;
        this.dualWriteService = dualWriteService;
    }

    /**
     * Gets all orders for active products.
     * The active product IDs are fetched using the dual-write service.
     */
    public List<Order> getOrdersForActiveProducts() {
        long startTime = System.currentTimeMillis();
        logger.info("Fetching orders for active products");

        // First get active product IDs (from Redis with MongoDB fallback)
        List<String> activeProductIds = dualWriteService.getDistinctActiveProductIds();

        if (activeProductIds.isEmpty()) {
            logger.warn("No active product IDs found");
            return List.of();
        }

        logger.info("Found {} active product IDs, fetching orders", activeProductIds.size());

        // Fetch orders from MongoDB for these product IDs
        List<Order> orders = orderRepository.findByProductIdIn(activeProductIds);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Found {} orders for active products in {} ms", orders.size(), duration);

        return orders;
    }

    /**
     * Gets all active products with orders.
     * Returns only product IDs that have at least one order.
     */
    public List<String> getActiveProductsWithOrders() {
        long startTime = System.currentTimeMillis();
        logger.info("Fetching active products with orders");

        // First get active product IDs (from Redis with MongoDB fallback)
        List<String> activeProductIds = dualWriteService.getDistinctActiveProductIds();

        if (activeProductIds.isEmpty()) {
            logger.warn("No active product IDs found");
            return List.of();
        }

        // Fetch only products that have at least one order
        List<String> productsWithOrders = orderRepository.findByProductIdIn(activeProductIds).stream()
                .map(Order::getProductId)
                .distinct()
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Found {} active products with orders in {} ms", productsWithOrders.size(), duration);

        return productsWithOrders;
    }

    /**
     * Gets orders for a specific product ID.
     * Only returns orders if the product is active.
     */
    public List<Order> getOrdersForProduct(String productId) {
        long startTime = System.currentTimeMillis();
        logger.info("Fetching orders for product ID: {}", productId);

        // Check if product is active
        List<String> activeProductIds = dualWriteService.getDistinctActiveProductIds();

        if (!activeProductIds.contains(productId)) {
            logger.warn("Product ID {} is not active", productId);
            return List.of();
        }

        // Fetch orders from MongoDB
        List<Order> orders = orderRepository.findByProductId(productId);

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Found {} orders for product ID {} in {} ms", orders.size(), productId, duration);

        return orders;
    }
}