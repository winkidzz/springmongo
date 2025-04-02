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
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.StringReader;

@Service
public class ElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchService.class);
    private static final int BATCH_SIZE = 100; // Small batch size to avoid timeout

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
    private ElasticsearchClient elasticsearchClient;

    /**
     * Debug method to test product configuration queries
     */
    public Map<String, Object> debugProductConfiguration(String productId) {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("Debugging product configuration for productId: {}", productId);

            // Let's check if Elasticsearch is working properly first
            try {
                logger.info("Testing basic Elasticsearch connectivity");
                SearchRequest testRequest = new SearchRequest.Builder()
                        .index("product_configs")
                        .size(1)
                        .build();

                SearchResponse<ProductConfigES> testResponse = elasticsearchClient.search(
                        testRequest, ProductConfigES.class);

                long totalConfigs = testResponse.hits().total() != null ? testResponse.hits().total().value() : 0;
                logger.info("Elasticsearch connectivity test: found {} total configs", totalConfigs);
                result.put("elasticsearch_connectivity", "OK");
                result.put("total_configs", totalConfigs);

                if (!testResponse.hits().hits().isEmpty()) {
                    result.put("sample_document", testResponse.hits().hits().get(0).source());
                }
            } catch (Exception e) {
                logger.error("Error testing Elasticsearch connectivity", e);
                result.put("elasticsearch_connectivity", "ERROR: " + e.getMessage());
            }

            // Check if the product exists in Elasticsearch using the raw format directly
            try {
                // First using the standard query
                SearchRequest searchRequest = new SearchRequest.Builder()
                        .index("product_configs")
                        .query(q -> q
                                .term(t -> t
                                        .field("productId.keyword")
                                        .value(productId)))
                        .build();

                SearchResponse<ProductConfigES> response = elasticsearchClient.search(
                        searchRequest, ProductConfigES.class);

                long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
                result.put("totalMatches", totalHits);
                result.put("productId", productId);

                List<ProductConfigES> configs = new ArrayList<>();
                for (Hit<ProductConfigES> hit : response.hits().hits()) {
                    if (hit.source() != null) {
                        configs.add(hit.source());
                    }
                }
                result.put("configurations", configs);
            } catch (Exception e) {
                logger.error("Error searching for product by ID", e);
                result.put("search_error", e.getMessage());
            }

            // Also check active configs for this product ID with raw JSON directly
            try {
                String rawQuery = String.format(
                        "{\"query\":{\"bool\":{\"must\":[" +
                                "{\"term\":{\"productId.keyword\":\"%s\"}}," +
                                "{\"term\":{\"enabled\":true}}," +
                                "{\"range\":{\"startDate\":{\"lte\":\"2025-04-01T00:00:00.000\"}}}," +
                                "{\"range\":{\"endDate\":{\"gte\":\"2025-04-01T00:00:00.000\"}}}" +
                                "]}}}",
                        productId);

                // Log the raw query for debugging
                logger.info("Raw query: {}", rawQuery);

                // Execute the raw query using the low-level API if available, or use a direct
                // HTTP call
                SearchRequest activeRequest = new SearchRequest.Builder()
                        .index("product_configs")
                        .withJson(new StringReader(rawQuery))
                        .build();

                SearchResponse<ProductConfigES> activeResponse = elasticsearchClient.search(
                        activeRequest, ProductConfigES.class);

                long activeHits = activeResponse.hits().total() != null ? activeResponse.hits().total().value() : 0;
                result.put("activeMatches", activeHits);

                List<ProductConfigES> activeConfigs = new ArrayList<>();
                for (Hit<ProductConfigES> hit : activeResponse.hits().hits()) {
                    if (hit.source() != null) {
                        activeConfigs.add(hit.source());
                        logger.info("Found active config: {}", hit.source());
                    }
                }
                result.put("activeConfigurations", activeConfigs);
            } catch (Exception e) {
                logger.error("Error searching for active product configs", e);
                result.put("active_search_error", e.getMessage());
            }

            // Check completed orders with this product ID
            try {
                SearchRequest orderRequest = new SearchRequest.Builder()
                        .index("orders")
                        .query(q -> q
                                .bool(b -> b
                                        .must(m -> m
                                                .term(t -> t
                                                        .field("productId.keyword")
                                                        .value(productId)))
                                        .must(m -> m
                                                .term(t -> t
                                                        .field("status.keyword")
                                                        .value("COMPLETED")))))
                        .build();

                SearchResponse<OrderES> orderResponse = elasticsearchClient.search(
                        orderRequest, OrderES.class);

                long orderHits = orderResponse.hits().total() != null ? orderResponse.hits().total().value() : 0;
                result.put("completedOrders", orderHits);

                if (!orderResponse.hits().hits().isEmpty()) {
                    result.put("sampleOrder", orderResponse.hits().hits().get(0).source());
                }
            } catch (Exception e) {
                logger.error("Error searching for completed orders", e);
                result.put("order_search_error", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Error debugging product configuration", e);
            result.put("error", e.getMessage());
        }

        return result;
    }

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
        int totalOrders = allOrders.size();
        logger.info("Found {} orders to synchronize", totalOrders);

        // Process in small batches
        for (int i = 0; i < totalOrders; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, totalOrders);
            List<OrderES> batchOrdersES = allOrders.subList(i, end).stream()
                    .map(OrderES::fromOrder)
                    .collect(Collectors.toList());

            try {
                orderESRepository.saveAll(batchOrdersES);
                logger.info("Synchronized batch {}-{} of {} orders to Elasticsearch",
                        i, end, totalOrders);
            } catch (Exception e) {
                logger.error("Error synchronizing batch {}-{}: {}", i, end, e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("Synchronized {} orders to Elasticsearch in {} ms",
                totalOrders, (endTime - startTime));
    }

    /**
     * Synchronize all product configurations from MongoDB to Elasticsearch
     */
    public void syncProductConfigsToElasticsearch() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting synchronization of product configs from MongoDB to Elasticsearch");

        List<ProductConfig> allConfigs = productConfigRepository.findAll();
        int totalConfigs = allConfigs.size();
        logger.info("Found {} product configs to synchronize", totalConfigs);

        // Process in small batches
        for (int i = 0; i < totalConfigs; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, totalConfigs);
            List<ProductConfigES> batchConfigsES = allConfigs.subList(i, end).stream()
                    .map(ProductConfigES::fromProductConfig)
                    .collect(Collectors.toList());

            try {
                productConfigESRepository.saveAll(batchConfigsES);
                logger.info("Synchronized batch {}-{} of {} product configs to Elasticsearch",
                        i, end, totalConfigs);
            } catch (Exception e) {
                logger.error("Error synchronizing batch {}-{}: {}", i, end, e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        logger.info("Synchronized {} product configs to Elasticsearch in {} ms",
                totalConfigs, (endTime - startTime));
    }

    /**
     * Find distinct active products using Elasticsearch repository
     */
    public List<String> findDistinctActiveProductsES() {
        logger.info("Finding distinct active products using Elasticsearch repository");
        return orderESRepositoryCustom.findDistinctActiveProductsES();
    }

    /**
     * Find distinct active products using native Elasticsearch client
     */
    public List<String> findDistinctActiveProductsESNative() {
        logger.info("Finding distinct active products using native Elasticsearch client");
        return orderESRepositoryCustom.findDistinctActiveProductsESNative();
    }

    /**
     * Find distinct active products using the most optimized approach for Redis
     * caching
     * This method measures and logs performance metrics
     */
    public List<String> findDistinctActiveProductsOptimizedForCache() {
        long startTime = System.currentTimeMillis();
        logger.info("Finding distinct active products using optimized approach for Redis caching");

        // Use the superfast implementation since it's the most optimized
        try {
            // Step 1: Get all completed orders
            long queryStartTime = System.currentTimeMillis();
            logger.info("Step 1: Querying completed orders");

            // Use a raw JSON query for maximum performance
            String orderQueryJson = "{\"query\":{\"term\":{\"status.keyword\":\"COMPLETED\"}}}";

            SearchRequest orderRequest = new SearchRequest.Builder()
                    .index("orders")
                    .withJson(new StringReader(orderQueryJson))
                    .size(0) // We don't need documents, just aggregation
                    .aggregations("products", a -> a
                            .terms(t -> t
                                    .field("productId.keyword")
                                    .size(10000))) // Get all unique product IDs
                    .build();

            SearchResponse<Void> orderResponse = elasticsearchClient.search(orderRequest, Void.class);

            // Extract product IDs from aggregation
            List<String> productIds = new ArrayList<>();

            if (orderResponse.aggregations() != null &&
                    orderResponse.aggregations().get("products") != null) {

                var agg = orderResponse.aggregations().get("products").sterms();
                if (agg != null && agg.buckets() != null && agg.buckets().array() != null) {
                    for (var bucket : agg.buckets().array()) {
                        if (bucket.key() != null) {
                            productIds.add(bucket.key().toString());
                        }
                    }
                }
            }

            long step1Time = System.currentTimeMillis() - queryStartTime;
            logger.info("Step 1 completed in {} ms, found {} distinct product IDs",
                    step1Time, productIds.size());

            if (productIds.isEmpty()) {
                logger.warn("No product IDs found in completed orders");
                return new ArrayList<>();
            }

            // Step 2: Find active product configurations
            long step2StartTime = System.currentTimeMillis();
            logger.info("Step 2: Finding active product configurations");

            LocalDateTime now = LocalDateTime.now();
            String formattedDate = now.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME);

            // Convert String List to List<FieldValue>
            List<co.elastic.clients.elasticsearch._types.FieldValue> fieldValues = productIds.stream()
                    .map(id -> co.elastic.clients.elasticsearch._types.FieldValue.of(id))
                    .collect(java.util.stream.Collectors.toList());

            // Build efficient query for active configurations
            SearchRequest configRequest = new SearchRequest.Builder()
                    .index("product_configs")
                    .size(productIds.size())
                    .source(s -> s.filter(f -> f.includes("productId")))
                    .query(q -> q
                            .bool(b -> {
                                // Terms query for all product IDs at once
                                b.must(m -> m
                                        .terms(t -> t
                                                .field("productId.keyword")
                                                .terms(ft -> ft.value(fieldValues))));

                                // Filter for enabled configurations
                                b.must(m -> m
                                        .term(t -> t
                                                .field("enabled")
                                                .value(true)));

                                // Filter for date range
                                b.must(m -> m
                                        .range(r -> r
                                                .field("startDate")
                                                .lte(JsonData.of(formattedDate))));

                                b.must(m -> m
                                        .range(r -> r
                                                .field("endDate")
                                                .gte(JsonData.of(formattedDate))));

                                return b;
                            }))
                    .build();

            // Execute query
            SearchResponse<JsonData> configResponse = elasticsearchClient.search(configRequest, JsonData.class);

            // Extract active product IDs
            List<String> activeProducts = new ArrayList<>();
            if (configResponse.hits().hits() != null) {
                for (Hit<JsonData> hit : configResponse.hits().hits()) {
                    if (hit.source() != null) {
                        try {
                            String productId = hit.source().to(Map.class).get("productId").toString();
                            activeProducts.add(productId);
                        } catch (Exception e) {
                            logger.warn("Error extracting product ID: {}", e.getMessage());
                        }
                    }
                }
            }

            long step2Time = System.currentTimeMillis() - step2StartTime;
            long totalTime = System.currentTimeMillis() - startTime;

            logger.info("Step 2 completed in {} ms, found {} active products", step2Time, activeProducts.size());
            logger.info("Total execution time: {} ms", totalTime);

            return activeProducts;

        } catch (Exception e) {
            logger.error("Error executing optimized query: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Generate large test data for performance testing
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
            order.setOrderDate(java.time.LocalDateTime.now().minusDays((int) (Math.random() * 90)));
            order.setStatus(Math.random() > 0.3 ? "COMPLETED" : "PENDING"); // 70% completed
            order.setProductId("PROD-" + (1 + (int) (Math.random() * numProducts)));
            order.setCustomerId("CUST-" + (1 + (int) (Math.random() * 1000)));
            order.setAmount(10 + Math.random() * 1000);
            order.setCreatedAt(java.time.LocalDateTime.now().minusDays((int) (Math.random() * 90)));
            order.setUpdatedAt(java.time.LocalDateTime.now());
            orders.add(order);

            ordersES.add(OrderES.fromOrder(order));
        }

        // Save in batches to prevent memory issues
        int batchSize = 100; // Smaller batch size
        for (int i = 0; i < numOrders; i += batchSize) {
            int end = Math.min(i + batchSize, numOrders);
            orderRepository.saveAll(orders.subList(i, end));
            orderESRepository.saveAll(ordersES.subList(i, end));
            logger.info("Saved batch {} to {}", i, end);
        }

        long endTime = System.currentTimeMillis();
        logger.info("Generated large test dataset in {} ms", (endTime - startTime));
    }
}