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
        // Clear existing data
        mongoTemplate.dropCollection(Order.class);
        mongoTemplate.dropCollection(ProductConfig.class);

        // Generate product configurations
        List<ProductConfig> productConfigs = generateProductConfigs(50);
        mongoTemplate.insertAll(productConfigs);

        // Generate orders
        List<Order> orders = generateOrders(1000, productConfigs);
        mongoTemplate.insertAll(orders);
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

    private List<Order> generateOrders(int count, List<ProductConfig> productConfigs) {
        List<Order> orders = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setOrderId("ORD-" + (i + 1));
            order.setOrderDate(now.minusDays(random.nextInt(30)));
            order.setStatus(random.nextBoolean() ? "COMPLETED" : "PENDING");
            order.setProductId(productConfigs.get(random.nextInt(productConfigs.size())).getProductId());
            orders.add(order);
        }

        return orders;
    }
}