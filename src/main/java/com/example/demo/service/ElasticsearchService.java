package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderES;
import com.example.demo.model.ProductConfig;
import com.example.demo.model.ProductConfigES;
import com.example.demo.repository.OrderESRepository;
import com.example.demo.repository.OrderESRepositoryCustom;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductConfigESRepository;
import com.example.demo.repository.ProductConfigRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderESRepository orderESRepository;

    @Autowired
    private ProductConfigRepository productConfigRepository;

    @Autowired
    private ProductConfigESRepository productConfigESRepository;

    @Autowired
    private OrderESRepositoryCustom orderESRepositoryCustom;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * Synchronize all MongoDB data to Elasticsearch
     */
    public void syncAllDataToElasticsearch() {
        syncOrdersToElasticsearch();
        syncProductConfigsToElasticsearch();
    }

    /**
     * Synchronize all orders from MongoDB to Elasticsearch
     */
    public void syncOrdersToElasticsearch() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting synchronization of orders from MongoDB to Elasticsearch");

        List<Order> allOrders = orderRepository.findAll();
        List<OrderES> orderESList = allOrders.stream()
                .map(OrderES::fromOrder)
                .collect(Collectors.toList());

        orderESRepository.saveAll(orderESList);

        long endTime = System.currentTimeMillis();
        logger.info("Synchronized {} orders to Elasticsearch in {} ms",
                orderESList.size(), (endTime - startTime));
    }

    /**
     * Synchronize all product configurations from MongoDB to Elasticsearch
     */
    public void syncProductConfigsToElasticsearch() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting synchronization of product configs from MongoDB to Elasticsearch");

        List<ProductConfig> allConfigs = productConfigRepository.findAll();
        List<ProductConfigES> configESList = allConfigs.stream()
                .map(ProductConfigES::fromProductConfig)
                .collect(Collectors.toList());

        productConfigESRepository.saveAll(configESList);

        long endTime = System.currentTimeMillis();
        logger.info("Synchronized {} product configs to Elasticsearch in {} ms",
                configESList.size(), (endTime - startTime));
    }

    /**
     * Get distinct active products using Elasticsearch
     */
    public List<String> findDistinctActiveProductsES() {
        return orderESRepositoryCustom.findDistinctActiveProductsES();
    }

    /**
     * Get distinct active products using Elasticsearch native query
     */
    public List<String> findDistinctActiveProductsESNative() {
        return orderESRepositoryCustom.findDistinctActiveProductsESNative();
    }

    /**
     * Generate large test dataset (100k orders and product configs)
     */
    public void generateLargeTestData(int numProducts, int numOrders) {
        long startTime = System.currentTimeMillis();
        logger.info("Generating large test dataset with {} products and {} orders", numProducts, numOrders);

        // Generate product configs
        List<ProductConfig> configs = new ArrayList<>();
        List<ProductConfigES> configsES = new ArrayList<>();

        for (int i = 1; i <= numProducts; i++) {
            ProductConfig config = new ProductConfig();
            config.setProductId("PROD-" + i);
            config.setEnabled(Math.random() > 0.3); // 70% are enabled
            config.setStartDate(java.time.LocalDateTime.now().minusDays((int) (Math.random() * 60)));
            config.setEndDate(java.time.LocalDateTime.now().plusDays((int) (Math.random() * 60)));
            configs.add(config);

            configsES.add(ProductConfigES.fromProductConfig(config));
        }

        // Save product configs
        productConfigRepository.saveAll(configs);
        productConfigESRepository.saveAll(configsES);

        // Generate orders
        List<Order> orders = new ArrayList<>();
        List<OrderES> ordersES = new ArrayList<>();

        for (int i = 1; i <= numOrders; i++) {
            Order order = new Order();
            order.setOrderId("ORD-" + i);
            order.setProductId("PROD-" + (1 + (int) (Math.random() * numProducts)));
            order.setCustomerId("CUST-" + (1 + (int) (Math.random() * 1000)));
            order.setAmount(10 + Math.random() * 990);
            order.setStatus(Math.random() > 0.5 ? "COMPLETED" : "PENDING");
            order.setCreatedAt(java.time.LocalDateTime.now().minusDays((int) (Math.random() * 30)));
            order.setUpdatedAt(java.time.LocalDateTime.now());
            orders.add(order);

            if (i % 10000 == 0) {
                // Save in batches to avoid memory issues
                orderRepository.saveAll(orders);
                orders.forEach(o -> ordersES.add(OrderES.fromOrder(o)));
                orderESRepository.saveAll(ordersES);

                orders.clear();
                ordersES.clear();

                logger.info("Processed {} orders so far", i);
            }
        }

        // Save any remaining orders
        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
            orders.forEach(o -> ordersES.add(OrderES.fromOrder(o)));
            orderESRepository.saveAll(ordersES);
        }

        long endTime = System.currentTimeMillis();
        logger.info("Generated large test dataset in {} ms", (endTime - startTime));
    }
}