package com.example.demo.util;

import com.example.demo.model.Order;
import com.example.demo.model.ProductConfig;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class TestDataGenerator {
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductConfigRepository productConfigRepository;

    private final Random random = new Random();

    public void generateTestData(int numOrders) {
        // Generate product configurations first
        List<ProductConfig> productConfigs = generateProductConfigs();
        productConfigRepository.saveAll(productConfigs);

        // Generate orders
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < numOrders; i++) {
            Order order = generateOrder(productConfigs);
            orders.add(order);
        }
        orderRepository.saveAll(orders);
    }

    private List<ProductConfig> generateProductConfigs() {
        List<ProductConfig> configs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Generate 10 product configurations
        for (int i = 1; i <= 10; i++) {
            ProductConfig config = new ProductConfig();
            config.setProductId("PROD-" + i);
            config.setEnabled(true);
            config.setStartDate(now.minusDays(30));
            config.setEndDate(now.plusDays(30));
            configs.add(config);
        }
        return configs;
    }

    private Order generateOrder(List<ProductConfig> productConfigs) {
        Order order = new Order();
        order.setProductId(productConfigs.get(random.nextInt(productConfigs.size())).getProductId());
        order.setCustomerId("CUST-" + (random.nextInt(1000) + 1));
        order.setAmount(random.nextDouble() * 1000);
        order.setStatus(random.nextBoolean() ? "COMPLETED" : "PENDING");
        order.setCreatedAt(LocalDateTime.now().minusDays(random.nextInt(30)));
        order.setUpdatedAt(order.getCreatedAt().plusHours(random.nextInt(24)));
        return order;
    }
} 