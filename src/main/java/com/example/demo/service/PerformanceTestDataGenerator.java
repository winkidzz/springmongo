package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.ProductConfig;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class PerformanceTestDataGenerator {

    @Autowired
    private ProductConfigRepository productConfigRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final int BATCH_SIZE = 1000;
    private static final Random RANDOM = new Random();

    public void generateTestData(int numOrders, int numConfigs) {
        log.info("Starting test data generation with {} orders and {} configs", numOrders, numConfigs);

        // First check if we already have enough data
        long existingOrders = orderRepository.count();
        if (existingOrders >= numOrders) {
            log.info("Already have {} orders, skipping generation", existingOrders);
            return;
        }

        // Generate configurations first
        List<ProductConfig> configs = generateConfigurations(numConfigs);

        // Generate orders in batches
        generateOrdersInBatches(numOrders, configs);

        // Display final statistics
        displayStatistics();
    }

    private List<ProductConfig> generateConfigurations(int numConfigs) {
        log.info("Generating {} configurations", numConfigs);
        List<ProductConfig> configs = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < numConfigs; i++) {
            ProductConfig config = new ProductConfig();
            config.setId("config-" + i);
            config.setProductId("product-" + i);
            config.setConfigName("Test Config " + i);
            config.setConfigValue("value-" + i);
            config.setEnabled(i % 2 == 0); // Alternate between enabled and disabled

            // Set various dates
            config.setStartDate(now.minusDays(30));
            config.setEndDate(now.plusDays(30));
            config.setReleaseStartDate(now.minusDays(15));
            config.setReleaseEndDate(now.plusDays(15));
            config.setCreatedStartDate(now.minusDays(45));
            config.setCreatedEndDate(now.plusDays(45));

            configs.add(config);
        }

        return productConfigRepository.saveAll(configs);
    }

    private void generateOrdersInBatches(int numOrders, List<ProductConfig> configs) {
        log.info("Generating {} orders in batches of {}", numOrders, BATCH_SIZE);
        AtomicInteger processedCount = new AtomicInteger(0);

        while (processedCount.get() < numOrders) {
            int batchSize = Math.min(BATCH_SIZE, numOrders - processedCount.get());
            List<Order> batch = new ArrayList<>();

            for (int i = 0; i < batchSize; i++) {
                Order order = createRandomOrder(configs);
                batch.add(order);
            }

            orderRepository.saveAll(batch);
            processedCount.addAndGet(batchSize);

            if (processedCount.get() % 10000 == 0) {
                log.info("Processed {} orders", processedCount.get());
            }
        }
    }

    private Order createRandomOrder(List<ProductConfig> configs) {
        Order order = new Order();
        ProductConfig randomConfig = configs.get(RANDOM.nextInt(configs.size()));

        order.setId("order-" + System.currentTimeMillis() + "-" + RANDOM.nextInt(1000));
        order.setProductId(randomConfig.getProductId());
        order.setProductConfigId(randomConfig.getId());
        order.setQuantity(RANDOM.nextInt(10) + 1);
        order.setPrice(RANDOM.nextDouble() * 100);
        order.setStatus(getRandomStatus());
        order.setOrderDate(LocalDateTime.now().minusDays(RANDOM.nextInt(60)));
        order.setDeliveryDate(order.getOrderDate().plusDays(RANDOM.nextInt(14) + 1));
        order.setProductName("Product " + randomConfig.getProductId());
        order.setProductCategory("Category " + RANDOM.nextInt(5));

        return order;
    }

    private String getRandomStatus() {
        String[] statuses = { "PENDING", "PROCESSING", "COMPLETED", "CANCELLED" };
        return statuses[RANDOM.nextInt(statuses.length)];
    }

    public void displayStatistics() {
        long totalOrders = orderRepository.count();
        long totalConfigs = productConfigRepository.count();

        long enabledConfigs = productConfigRepository.countByEnabled(true);

        log.info("=== Test Data Statistics ===");
        log.info("Total Orders: {}", totalOrders);
        log.info("Total Configs: {}", totalConfigs);
        log.info("Enabled Configs: {}", enabledConfigs);

        // Display order distribution by config
        log.info("\nOrder Distribution by Config:");
        productConfigRepository.findAll().forEach(config -> {
            long orderCount = orderRepository.countByProductConfigId(config.getId());
            log.info("Config {} (enabled={}): {} orders",
                    config.getId(), config.isEnabled(), orderCount);
        });
    }

    public void clearTestData() {
        log.info("Clearing all test data");
        orderRepository.deleteAll();
        productConfigRepository.deleteAll();
        log.info("Test data cleared successfully");
    }
}