package com.example.demo.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.model.Order;
import com.example.demo.model.ProductConfig;

@Component
public class DataGenerator implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;
    private final Random random = new Random();

    public DataGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        // Don't auto-generate data on startup to avoid overwriting existing data
    }

    public void generateTestData() {
        // Clear existing data
        mongoTemplate.dropCollection(Order.class);
        mongoTemplate.dropCollection(ProductConfig.class);

        // Generate product configurations
        List<ProductConfig> productConfigs = generateProductConfigs(100);
        mongoTemplate.insertAll(productConfigs);
        System.out.println("Generated " + productConfigs.size() + " product configurations");

        // Generate orders in batches of 10,000 to avoid memory issues
        int totalOrders = 100000;
        int batchSize = 10000;
        for (int i = 0; i < totalOrders; i += batchSize) {
            int currentBatchSize = Math.min(batchSize, totalOrders - i);
            List<Order> orderBatch = generateOrders(currentBatchSize, productConfigs, i);
            mongoTemplate.insertAll(orderBatch);
            System.out.println("Generated " + (i + currentBatchSize) + " orders");
        }
    }

    private List<ProductConfig> generateProductConfigs(int count) {
        List<ProductConfig> configs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            ProductConfig config = new ProductConfig();
            config.setProductId("PROD-" + (i + 1));
            config.setEnabled(random.nextBoolean());
            config.setStartDate(now.minusDays(random.nextInt(30)));
            config.setEndDate(now.plusDays(random.nextInt(30)));
            configs.add(config);
        }

        return configs;
    }

    private List<Order> generateOrders(int count, List<ProductConfig> productConfigs, int startIndex) {
        List<Order> orders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setOrderId("ORD-" + (startIndex + i + 1));
            order.setOrderDate(now.minusDays(random.nextInt(30)));
            order.setStatus(random.nextBoolean() ? "COMPLETED" : "PENDING");
            order.setProductId(productConfigs.get(random.nextInt(productConfigs.size())).getProductId());
            order.setCustomerId("CUST-" + (random.nextInt(1000) + 1));
            order.setAmount(10.0 + random.nextDouble() * 990.0); // Random amount between $10 and $1000
            order.setCreatedAt(order.getOrderDate());
            order.setUpdatedAt(now);
            orders.add(order);
        }

        return orders;
    }
}