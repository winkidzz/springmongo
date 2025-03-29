package com.example.demo.config;

import com.example.demo.model.Order;
import com.example.demo.model.ProductConfig;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductConfigRepository productConfigRepository;

    @Override
    public void run(String... args) {
        // Generate 100k orders
        List<Order> orders = new ArrayList<>();
        Random random = new Random();
        String[] categories = { "Electronics", "Clothing", "Books", "Food", "Sports" };
        String[] statuses = { "Pending", "Processing", "Shipped", "Delivered", "Cancelled" };

        for (int i = 0; i < 100000; i++) {
            Order order = new Order();
            order.setProductId("PROD" + (random.nextInt(20) + 1));
            order.setOrderNumber("ORD" + (i + 1));
            order.setOrderDate(LocalDateTime.now().minusDays(random.nextInt(365)));
            order.setDeliveryDate(LocalDateTime.now().plusDays(random.nextInt(30)));
            order.setProductName("Product " + (random.nextInt(20) + 1));
            order.setProductCategory(categories[random.nextInt(categories.length)]);
            order.setPrice(random.nextDouble() * 1000);
            order.setQuantity(random.nextInt(10) + 1);
            order.setStatus(statuses[random.nextInt(statuses.length)]);
            orders.add(order);
        }
        orderRepository.saveAll(orders);

        // Generate 20 products with 100 configurations each
        List<ProductConfig> configs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int productId = 1; productId <= 20; productId++) {
            for (int configId = 1; configId <= 100; configId++) {
                ProductConfig config = new ProductConfig();
                config.setProductId("PROD" + productId);
                config.setConfigName("Config" + configId);
                config.setConfigValue("Value" + configId);

                // Set random date range
                int daysOffset = random.nextInt(365);
                config.setStartDate(now.minusDays(daysOffset));
                config.setEndDate(now.plusDays(random.nextInt(365)));

                // 50% of configs are enabled
                config.setEnabled(random.nextBoolean());
                configs.add(config);
            }
        }
        productConfigRepository.saveAll(configs);
    }
}