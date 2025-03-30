package com.example.demo.config;

import com.example.demo.model.Order;
import com.example.demo.model.ProductConfig;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Configuration
public class DataLoader {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductConfigRepository productConfigRepository;

    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            System.out.println("Starting data load...");

            // Clear existing data
            orderRepository.deleteAll();
            productConfigRepository.deleteAll();

            // Insert product configurations
            List<ProductConfig> productConfigs = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate = now.minusDays(10);
            LocalDateTime endDate = now.plusDays(10);

            System.out.println("Creating product configurations...");
            for (int i = 0; i < 5; i++) {
                ProductConfig config = new ProductConfig();
                config.setProductId("PROD" + i);
                config.setConfigName("Config" + i);
                config.setConfigValue("Value" + i);
                config.setStartDate(startDate);
                config.setEndDate(endDate);
                config.setEnabled(true);
                productConfigs.add(config);
            }

            productConfigRepository.saveAll(productConfigs);
            System.out.println("Product configs saved: " + productConfigRepository.count());

            // Insert orders
            List<Order> orders = new ArrayList<>();
            String[] statuses = { "PENDING", "PROCESSING", "CANCELLED" };
            String[] productNames = { "Product A", "Product B", "Product C", "Product D", "Product E" };
            Random random = new Random();

            System.out.println("Creating orders...");
            for (int i = 0; i < 50; i++) {
                Order order = new Order();
                // Ensure productId matches with product configs
                order.setProductId("PROD" + random.nextInt(5));
                order.setOrderNumber("ORD" + i);
                // Ensure order date is within last 10 days
                order.setOrderDate(startDate.plusDays(random.nextInt(10)));
                order.setProductName(productNames[random.nextInt(productNames.length)]);
                order.setProductCategory("Category" + random.nextInt(5));
                // Ensure price is always > 10
                order.setPrice(random.nextDouble() * 100 + 10);
                order.setQuantity(random.nextInt(10) + 1);
                // Ensure status is one of the required values
                order.setStatus(statuses[random.nextInt(statuses.length)]);
                orders.add(order);
            }

            orderRepository.saveAll(orders);
            System.out.println("Orders saved: " + orderRepository.count());

            // Verify data
            System.out.println("\nVerifying data...");
            System.out.println("Sample order:");
            orderRepository.findAll().stream().findFirst().ifPresent(order -> {
                System.out.println("Order ID: " + order.getId());
                System.out.println("Product ID: " + order.getProductId());
                System.out.println("Status: " + order.getStatus());
                System.out.println("Price: " + order.getPrice());
                System.out.println("Order Date: " + order.getOrderDate());
            });

            System.out.println("\nSample product config:");
            productConfigRepository.findAll().stream().findFirst().ifPresent(config -> {
                System.out.println("Config ID: " + config.getId());
                System.out.println("Product ID: " + config.getProductId());
                System.out.println("Enabled: " + config.isEnabled());
                System.out.println("Start Date: " + config.getStartDate());
                System.out.println("End Date: " + config.getEndDate());
            });
        };
    }
}