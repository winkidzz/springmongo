package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.ProductConfig;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DataLoadService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductConfigRepository productConfigRepository;

    private final AtomicInteger loadCount = new AtomicInteger(0);

    public String loadLargeDataSet() {
        System.out.println("Starting large data load...");
        long startTime = System.currentTimeMillis();

        // Clear existing data
        orderRepository.deleteAll();
        productConfigRepository.deleteAll();

        // Insert product configurations
        List<ProductConfig> productConfigs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(10);
        LocalDateTime endDate = now.plusDays(10);

        System.out.println("Creating product configurations...");
        for (int i = 0; i < 20; i++) {
            ProductConfig config = new ProductConfig();
            config.setProductId("PROD" + i);
            config.setConfigName("Config" + i);
            config.setConfigValue("Value" + i);
            config.setStartDate(startDate);
            config.setEndDate(endDate);
            // Enable only first 10 products
            config.setEnabled(i < 10);
            productConfigs.add(config);
        }

        productConfigRepository.saveAll(productConfigs);
        System.out.println("Product configs saved: " + productConfigRepository.count());

        // Insert orders in batches
        List<Order> orders = new ArrayList<>();
        String[] statuses = { "PENDING", "PROCESSING", "CANCELLED" };
        String[] productNames = {
                "Product A", "Product B", "Product C", "Product D", "Product E",
                "Product F", "Product G", "Product H", "Product I", "Product J",
                "Product K", "Product L", "Product M", "Product N", "Product O",
                "Product P", "Product Q", "Product R", "Product S", "Product T"
        };
        Random random = new Random();

        System.out.println("Creating orders...");
        int batchSize = 10000;
        int totalOrders = 200000;

        for (int i = 0; i < totalOrders; i++) {
            Order order = new Order();
            // Use only enabled products (first 10)
            order.setProductId("PROD" + random.nextInt(10));
            order.setOrderNumber("ORD" + i);
            order.setOrderDate(startDate.plusDays(random.nextInt(10)));
            order.setProductName(productNames[random.nextInt(productNames.length)]);
            order.setProductCategory("Category" + random.nextInt(5));
            order.setPrice(random.nextDouble() * 100 + 10);
            order.setQuantity(random.nextInt(10) + 1);
            order.setStatus(statuses[random.nextInt(statuses.length)]);
            orders.add(order);

            // Save in batches
            if (orders.size() >= batchSize) {
                orderRepository.saveAll(orders);
                orders.clear();
                System.out.println("Saved batch of " + batchSize + " orders. Progress: " + (i + 1) + "/" + totalOrders);
            }
        }

        // Save remaining orders
        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        loadCount.incrementAndGet();

        String summary = String.format(
                "Data load completed in %d seconds\n" +
                        "Total orders: %d\n" +
                        "Total product configs: %d\n" +
                        "Enabled products: 10\n" +
                        "Disabled products: 10\n" +
                        "Load count: %d",
                duration / 1000,
                orderRepository.count(),
                productConfigRepository.count(),
                loadCount.get());

        System.out.println(summary);
        return summary;
    }

    public String getDataStats() {
        return String.format(
                "Current data statistics:\n" +
                        "Total orders: %d\n" +
                        "Total product configs: %d\n" +
                        "Load count: %d",
                orderRepository.count(),
                productConfigRepository.count(),
                loadCount.get());
    }
}