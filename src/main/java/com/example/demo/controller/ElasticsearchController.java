package com.example.demo.controller;

import com.example.demo.service.ElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.example.demo.model.OrderES;
import com.example.demo.model.ProductConfigES;
import com.example.demo.model.Order;
import com.example.demo.model.ProductConfig;
import com.example.demo.repository.OrderESRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductConfigESRepository;
import com.example.demo.repository.ProductConfigRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.StringReader;

@RestController
@RequestMapping("/api/elasticsearch")
public class ElasticsearchController {

        private static final Logger logger = LoggerFactory.getLogger(ElasticsearchController.class);

        @Autowired
        private ElasticsearchService elasticsearchService;

        @Autowired
        private ElasticsearchClient elasticsearchClient;

        @Autowired
        private OrderRepository orderRepository;

        @Autowired
        private OrderESRepository orderESRepository;

        @Autowired
        private ProductConfigRepository productConfigRepository;

        @Autowired
        private ProductConfigESRepository productConfigESRepository;

        @Value("${elasticsearch.host:localhost}")
        private String elasticsearchHost;

        @Value("${elasticsearch.port:9200}")
        private int elasticsearchPort;

        @PostMapping("/sync")
        public ResponseEntity<Map<String, String>> syncData() {
                elasticsearchService.syncAllDataToElasticsearch();
                return ResponseEntity.ok(Map.of("message", "Data synchronized to Elasticsearch"));
        }

        @GetMapping("/active-products")
        public ResponseEntity<List<String>> getActiveProductsES() {
                return ResponseEntity.ok(elasticsearchService.findDistinctActiveProductsES());
        }

        @PostMapping("/generate-fixed-data")
        public ResponseEntity<Map<String, String>> generateFixedTestData() {
                try {
                        logger.info("Generating fixed test data for debugging");

                        // Create a fixed product config with dates that will work with our test
                        ProductConfig config = new ProductConfig();
                        config.setProductId("TEST-PROD-1");
                        config.setEnabled(true);
                        config.setStartDate(LocalDateTime.parse("2025-02-01T00:00:00"));
                        config.setEndDate(LocalDateTime.parse("2025-05-01T00:00:00"));
                        productConfigRepository.save(config);

                        ProductConfigES configES = ProductConfigES.fromProductConfig(config);
                        productConfigESRepository.save(configES);

                        // Create a fixed order with COMPLETED status
                        Order order = new Order();
                        order.setOrderId("TEST-ORD-1");
                        order.setOrderDate(LocalDateTime.parse("2025-03-01T12:00:00"));
                        order.setStatus("COMPLETED");
                        order.setProductId("TEST-PROD-1");
                        order.setCustomerId("TEST-CUST-1");
                        order.setAmount(100.0);
                        order.setCreatedAt(LocalDateTime.now());
                        order.setUpdatedAt(LocalDateTime.now());
                        orderRepository.save(order);

                        OrderES orderES = OrderES.fromOrder(order);
                        orderESRepository.save(orderES);

                        logger.info("Created test product config: {}", config);
                        logger.info("Created test order: {}", order);

                        return ResponseEntity.ok(Map.of("message", "Generated fixed test data"));
                } catch (Exception e) {
                        logger.error("Error generating fixed test data", e);
                        return ResponseEntity.ok(Map.of("error", e.getMessage()));
                }
        }

        @PostMapping("/generate-fixed-data-direct")
        public ResponseEntity<Map<String, String>> generateFixedTestDataDirect() {
                try {
                        logger.info("Generating fixed test data directly in Elasticsearch");

                        // Insert a test product config directly into Elasticsearch
                        String productConfigId = "test-config-" + System.currentTimeMillis();
                        String productConfigJson = String.format(
                                        "{" +
                                                        "\"_class\": \"com.example.demo.model.ProductConfigES\"," +
                                                        "\"productId\": \"TEST-PROD-DIRECT\"," +
                                                        "\"enabled\": true," +
                                                        "\"startDate\": \"2025-02-01T00:00:00.000\"," +
                                                        "\"endDate\": \"2025-05-01T00:00:00.000\"" +
                                                        "}");

                        co.elastic.clients.elasticsearch.core.IndexRequest<Object> indexConfigRequest = new co.elastic.clients.elasticsearch.core.IndexRequest.Builder<>()
                                        .index("product_configs")
                                        .id(productConfigId)
                                        .withJson(new StringReader(productConfigJson))
                                        .build();

                        co.elastic.clients.elasticsearch.core.IndexResponse indexConfigResponse = elasticsearchClient
                                        .index(indexConfigRequest);

                        logger.info("Product config indexed with id: {}, result: {}",
                                        indexConfigResponse.id(), indexConfigResponse.result());

                        // Insert a test order directly into Elasticsearch
                        String orderId = "test-order-" + System.currentTimeMillis();
                        String orderJson = String.format(
                                        "{" +
                                                        "\"_class\": \"com.example.demo.model.OrderES\"," +
                                                        "\"orderId\": \"TEST-ORD-DIRECT\"," +
                                                        "\"orderDate\": \"2025-03-01T12:00:00.000\"," +
                                                        "\"status\": \"COMPLETED\"," +
                                                        "\"productId\": \"TEST-PROD-DIRECT\"," +
                                                        "\"customerId\": \"TEST-CUST-DIRECT\"," +
                                                        "\"amount\": 100.0," +
                                                        "\"createdAt\": \"2025-04-02T00:00:00.000\"," +
                                                        "\"updatedAt\": \"2025-04-02T00:00:00.000\"" +
                                                        "}");

                        co.elastic.clients.elasticsearch.core.IndexRequest<Object> indexOrderRequest = new co.elastic.clients.elasticsearch.core.IndexRequest.Builder<>()
                                        .index("orders")
                                        .id(orderId)
                                        .withJson(new StringReader(orderJson))
                                        .build();

                        co.elastic.clients.elasticsearch.core.IndexResponse indexOrderResponse = elasticsearchClient
                                        .index(indexOrderRequest);

                        logger.info("Order indexed with id: {}, result: {}",
                                        indexOrderResponse.id(), indexOrderResponse.result());

                        // Force a refresh to make the documents immediately available for search
                        co.elastic.clients.elasticsearch.indices.RefreshRequest refreshRequest1 = new co.elastic.clients.elasticsearch.indices.RefreshRequest.Builder()
                                        .index("product_configs")
                                        .build();

                        co.elastic.clients.elasticsearch.indices.RefreshRequest refreshRequest2 = new co.elastic.clients.elasticsearch.indices.RefreshRequest.Builder()
                                        .index("orders")
                                        .build();

                        elasticsearchClient.indices().refresh(refreshRequest1);
                        elasticsearchClient.indices().refresh(refreshRequest2);

                        logger.info("Indices refreshed");

                        return ResponseEntity.ok(Map.of(
                                        "message", "Generated fixed test data directly in Elasticsearch",
                                        "productConfigId", productConfigId,
                                        "orderId", orderId));

                } catch (Exception e) {
                        logger.error("Error generating fixed test data directly", e);
                        return ResponseEntity.ok(Map.of("error", e.getMessage()));
                }
        }

        @GetMapping("/active-products-direct")
        public ResponseEntity<List<String>> getActiveProductsDirect() {
                try {
                        List<String> result = new ArrayList<>();
                        logger.info("Starting direct Elasticsearch query for active products using {}:{}",
                                        elasticsearchHost,
                                        elasticsearchPort);

                        // Step 1: Get all completed orders
                        SearchRequest orderRequest = new SearchRequest.Builder()
                                        .index("orders")
                                        .query(q -> q
                                                        .term(t -> t
                                                                        .field("status.keyword")
                                                                        .value("COMPLETED")))
                                        .size(10000)
                                        .build();

                        SearchResponse<OrderES> orderResponse = elasticsearchClient.search(orderRequest, OrderES.class);
                        logger.info("Found {} completed orders",
                                        orderResponse.hits().total() != null ? orderResponse.hits().total().value()
                                                        : 0);

                        if (orderResponse.hits().hits().isEmpty()) {
                                logger.warn("No completed orders found");
                                return ResponseEntity.ok(result);
                        }

                        // Log a sample hit to verify the format
                        Hit<OrderES> sampleOrderHit = orderResponse.hits().hits().get(0);
                        logger.info("Sample order hit: {}", sampleOrderHit.source());

                        Set<String> productIds = new HashSet<>();
                        for (Hit<OrderES> hit : orderResponse.hits().hits()) {
                                if (hit.source() != null) {
                                        productIds.add(hit.source().getProductId());
                                }
                        }

                        if (productIds.isEmpty()) {
                                logger.warn("No product IDs found in completed orders");
                                return ResponseEntity.ok(result);
                        }

                        logger.info("Found {} distinct product IDs from completed orders: {}", productIds.size(),
                                        productIds);

                        // First, get a sample product config to see the date format
                        SearchRequest sampleRequest = new SearchRequest.Builder()
                                        .index("product_configs")
                                        .size(1)
                                        .build();

                        SearchResponse<ProductConfigES> sampleResponse = elasticsearchClient.search(sampleRequest,
                                        ProductConfigES.class);

                        if (sampleResponse.hits().hits().isEmpty()) {
                                logger.warn("No product configs found in Elasticsearch");
                                return ResponseEntity.ok(result);
                        }

                        Hit<ProductConfigES> sampleConfigHit = sampleResponse.hits().hits().get(0);
                        logger.info("Sample product config: {}", sampleConfigHit.source());

                        // Step 2: Get active product configurations for these IDs
                        String formattedDate = "2025-04-01T00:00:00.000"; // Include the full time format as seen in
                                                                          // Elasticsearch

                        for (String productId : productIds) {
                                logger.info("Querying for product: {}", productId);
                                SearchRequest configRequest = new SearchRequest.Builder()
                                                .index("product_configs")
                                                .query(q -> q
                                                                .bool(b -> b
                                                                                .must(m -> m
                                                                                                .term(t -> t
                                                                                                                .field("productId.keyword")
                                                                                                                .value(productId)))
                                                                                .must(m -> m
                                                                                                .term(t -> t
                                                                                                                .field("enabled")
                                                                                                                .value(true)))
                                                                                .must(m -> m
                                                                                                .range(r -> r
                                                                                                                .field("startDate")
                                                                                                                .lte(JsonData.of(
                                                                                                                                formattedDate))))
                                                                                .must(m -> m
                                                                                                .range(r -> r
                                                                                                                .field("endDate")
                                                                                                                .gte(JsonData.of(
                                                                                                                                formattedDate))))))
                                                .build();

                                logger.info("Search request for {}: {}", productId, configRequest.toString());
                                SearchResponse<ProductConfigES> configResponse = elasticsearchClient.search(
                                                configRequest,
                                                ProductConfigES.class);
                                logger.info("Search response for {}: {} hits", productId,
                                                configResponse.hits().total() != null
                                                                ? configResponse.hits().total().value()
                                                                : 0);

                                if (configResponse.hits().total() != null
                                                && configResponse.hits().total().value() > 0) {
                                        logger.info("Found active configuration for product ID: {}", productId);
                                        result.add(productId);
                                }
                        }

                        // Let's also specifically check our test product
                        if (productIds.contains("TEST-PROD-1") || true) { // Always check our test product
                                SearchRequest testRequest = new SearchRequest.Builder()
                                                .index("product_configs")
                                                .query(q -> q
                                                                .bool(b -> b
                                                                                .must(m -> m
                                                                                                .term(t -> t
                                                                                                                .field("productId.keyword")
                                                                                                                .value("TEST-PROD-1")))
                                                                                .must(m -> m
                                                                                                .term(t -> t
                                                                                                                .field("enabled")
                                                                                                                .value(true)))
                                                                                .must(m -> m
                                                                                                .range(r -> r
                                                                                                                .field("startDate")
                                                                                                                .lte(JsonData.of(
                                                                                                                                formattedDate))))
                                                                                .must(m -> m
                                                                                                .range(r -> r
                                                                                                                .field("endDate")
                                                                                                                .gte(JsonData.of(
                                                                                                                                formattedDate))))))
                                                .build();

                                logger.info("Special search request for TEST-PROD-1: {}", testRequest.toString());
                                SearchResponse<ProductConfigES> testResponse = elasticsearchClient.search(testRequest,
                                                ProductConfigES.class);
                                logger.info("Special search response for TEST-PROD-1: {} hits",
                                                testResponse.hits().total() != null
                                                                ? testResponse.hits().total().value()
                                                                : 0);

                                if (testResponse.hits().total() != null && testResponse.hits().total().value() > 0) {
                                        logger.info("Found active configuration for TEST-PROD-1");
                                        result.add("TEST-PROD-1");
                                } else {
                                        logger.warn("No active configuration found for TEST-PROD-1");
                                }
                        }

                        logger.info("Found {} active products", result.size());
                        return ResponseEntity.ok(result);
                } catch (Exception e) {
                        logger.error("Error executing direct Elasticsearch query", e);
                        return ResponseEntity.ok(new ArrayList<>());
                }
        }

        @GetMapping("/active-products-native")
        public ResponseEntity<List<String>> getActiveProductsESNative() {
                return ResponseEntity.ok(elasticsearchService.findDistinctActiveProductsESNative());
        }

        @GetMapping("/debug/product/{productId}")
        public ResponseEntity<Map<String, Object>> debugProduct(@PathVariable String productId) {
                return ResponseEntity.ok(elasticsearchService.debugProductConfiguration(productId));
        }

        @PostMapping("/generate-test-data")
        public ResponseEntity<Map<String, String>> generateTestData(
                        @RequestParam(defaultValue = "100") int numProducts,
                        @RequestParam(defaultValue = "100000") int numOrders) {
                elasticsearchService.generateLargeTestData(numProducts, numOrders);
                return ResponseEntity.ok(Map.of(
                                "message",
                                "Generated " + numOrders + " orders and " + numProducts + " product configs"));
        }

        @GetMapping("/active-products-raw")
        public ResponseEntity<List<String>> getActiveProductsRaw() {
                try {
                        List<String> result = new ArrayList<>();
                        logger.info("Starting raw JSON query for active products");

                        // Step 1: Get all completed orders using raw JSON query
                        String orderQuery = "{\"query\":{\"term\":{\"status.keyword\":\"COMPLETED\"}},\"size\":10000}";

                        SearchRequest orderRequest = new SearchRequest.Builder()
                                        .index("orders")
                                        .withJson(new StringReader(orderQuery))
                                        .build();

                        SearchResponse<OrderES> orderResponse = elasticsearchClient.search(orderRequest, OrderES.class);
                        logger.info("Found {} completed orders",
                                        orderResponse.hits().total() != null ? orderResponse.hits().total().value()
                                                        : 0);

                        if (orderResponse.hits().hits().isEmpty()) {
                                logger.warn("No completed orders found");
                                return ResponseEntity.ok(result);
                        }

                        Set<String> productIds = new HashSet<>();
                        for (Hit<OrderES> hit : orderResponse.hits().hits()) {
                                if (hit.source() != null) {
                                        productIds.add(hit.source().getProductId());
                                }
                        }

                        logger.info("Found {} distinct product IDs from completed orders: {}", productIds.size(),
                                        productIds);

                        // Step 2: Check each product ID directly with raw query
                        String dateFormatted = "2025-04-01T00:00:00.000";

                        for (String productId : productIds) {
                                String configQuery = String.format(
                                                "{\"query\":{\"bool\":{\"must\":[" +
                                                                "{\"term\":{\"productId.keyword\":\"%s\"}}," +
                                                                "{\"term\":{\"enabled\":true}}," +
                                                                "{\"range\":{\"startDate\":{\"lte\":\"%s\"}}}," +
                                                                "{\"range\":{\"endDate\":{\"gte\":\"%s\"}}}" +
                                                                "]}}}",
                                                productId, dateFormatted, dateFormatted);

                                logger.info("Config query for {}: {}", productId, configQuery);

                                SearchRequest configRequest = new SearchRequest.Builder()
                                                .index("product_configs")
                                                .withJson(new StringReader(configQuery))
                                                .build();

                                SearchResponse<ProductConfigES> configResponse = elasticsearchClient.search(
                                                configRequest,
                                                ProductConfigES.class);

                                long hits = configResponse.hits().total() != null
                                                ? configResponse.hits().total().value()
                                                : 0;
                                logger.info("Found {} active configurations for product {}", hits, productId);

                                if (hits > 0) {
                                        result.add(productId);
                                }
                        }

                        // Also test for our specific test product
                        String testConfigQuery = String.format(
                                        "{\"query\":{\"bool\":{\"must\":[" +
                                                        "{\"term\":{\"productId.keyword\":\"TEST-PROD-1\"}}," +
                                                        "{\"term\":{\"enabled\":true}}," +
                                                        "{\"range\":{\"startDate\":{\"lte\":\"%s\"}}}," +
                                                        "{\"range\":{\"endDate\":{\"gte\":\"%s\"}}}" +
                                                        "]}}}",
                                        dateFormatted, dateFormatted);

                        logger.info("Test config query: {}", testConfigQuery);

                        SearchRequest testConfigRequest = new SearchRequest.Builder()
                                        .index("product_configs")
                                        .withJson(new StringReader(testConfigQuery))
                                        .build();

                        SearchResponse<ProductConfigES> testConfigResponse = elasticsearchClient.search(
                                        testConfigRequest,
                                        ProductConfigES.class);

                        long testHits = testConfigResponse.hits().total() != null
                                        ? testConfigResponse.hits().total().value()
                                        : 0;
                        logger.info("Found {} active configurations for test product", testHits);

                        if (testHits > 0) {
                                result.add("TEST-PROD-1");
                        }

                        logger.info("Final result: found {} active products", result.size());
                        return ResponseEntity.ok(result);
                } catch (Exception e) {
                        logger.error("Error executing raw JSON query", e);
                        return ResponseEntity.ok(new ArrayList<>());
                }
        }

        @GetMapping("/active-products-simple")
        public ResponseEntity<List<String>> getActiveProductsSimple() {
                try {
                        List<String> result = new ArrayList<>();
                        logger.info("Starting simplified Elasticsearch query for active products");

                        // Get current date in correct format
                        LocalDateTime now = LocalDateTime.now();
                        String formattedDate = now.format(DateTimeFormatter.ISO_DATE_TIME);
                        logger.info("Current date (formatted): {}", formattedDate);

                        // Step 1: Get all completed orders - using term query on keyword field
                        String orderQueryJson = String.format(
                                        "{\"query\":{\"term\":{\"status.keyword\":\"COMPLETED\"}}}");

                        SearchRequest orderRequest = new SearchRequest.Builder()
                                        .index("orders")
                                        .withJson(new StringReader(orderQueryJson))
                                        .size(10000)
                                        .build();

                        SearchResponse<OrderES> orderResponse = elasticsearchClient.search(orderRequest, OrderES.class);
                        logger.info("Found {} completed orders",
                                        orderResponse.hits().total() != null ? orderResponse.hits().total().value()
                                                        : 0);

                        if (orderResponse.hits().total() == null || orderResponse.hits().total().value() == 0) {
                                logger.warn("No completed orders found");
                                return ResponseEntity.ok(result);
                        }

                        // Extract product IDs from orders
                        Set<String> productIds = new HashSet<>();
                        for (Hit<OrderES> hit : orderResponse.hits().hits()) {
                                OrderES order = hit.source();
                                if (order != null && order.getProductId() != null) {
                                        logger.info("Found order for product: {}", order.getProductId());
                                        productIds.add(order.getProductId());
                                }
                        }

                        for (String productId : productIds) {
                                logger.info("Checking product: {}", productId);

                                // Direct JSON query for active configs
                                String configQueryJson = String.format(
                                                "{\"query\":{\"bool\":{\"must\":[" +
                                                                "{\"term\":{\"productId.keyword\":\"%s\"}}," +
                                                                "{\"term\":{\"enabled\":true}}," +
                                                                "{\"range\":{\"startDate\":{\"lte\":\"%s\"}}}," +
                                                                "{\"range\":{\"endDate\":{\"gte\":\"%s\"}}}" +
                                                                "]}}}",
                                                productId, formattedDate, formattedDate);

                                logger.info("Config query: {}", configQueryJson);

                                SearchRequest configRequest = new SearchRequest.Builder()
                                                .index("product_configs")
                                                .withJson(new StringReader(configQueryJson))
                                                .build();

                                SearchResponse<ProductConfigES> configResponse = elasticsearchClient.search(
                                                configRequest,
                                                ProductConfigES.class);

                                long hits = configResponse.hits().total() != null
                                                ? configResponse.hits().total().value()
                                                : 0;
                                logger.info("Found {} active configs for product {}", hits, productId);

                                if (hits > 0) {
                                        result.add(productId);
                                }
                        }

                        // Also test for our test product
                        String testQuery = String.format(
                                        "{\"query\":{\"bool\":{\"must\":[" +
                                                        "{\"term\":{\"productId.keyword\":\"TEST-PROD-DIRECT\"}}," +
                                                        "{\"term\":{\"enabled\":true}}," +
                                                        "{\"range\":{\"startDate\":{\"lte\":\"%s\"}}}," +
                                                        "{\"range\":{\"endDate\":{\"gte\":\"%s\"}}}" +
                                                        "]}}}",
                                        formattedDate, formattedDate);

                        logger.info("Test product query: {}", testQuery);

                        SearchRequest testRequest = new SearchRequest.Builder()
                                        .index("product_configs")
                                        .withJson(new StringReader(testQuery))
                                        .build();

                        SearchResponse<ProductConfigES> testResponse = elasticsearchClient.search(testRequest,
                                        ProductConfigES.class);

                        long testHits = testResponse.hits().total() != null ? testResponse.hits().total().value() : 0;
                        logger.info("Found {} active configs for test product", testHits);

                        if (testHits > 0) {
                                result.add("TEST-PROD-DIRECT");
                        }

                        logger.info("Final result: {} active products", result.size());
                        return ResponseEntity.ok(result);
                } catch (Exception e) {
                        logger.error("Error executing simplified Elasticsearch query", e);
                        return ResponseEntity.ok(new ArrayList<>());
                }
        }

        @GetMapping("/active-products-manual")
        public ResponseEntity<List<String>> getActiveProductsManual() {
                try {
                        List<String> result = new ArrayList<>();
                        logger.info("Starting manual Elasticsearch query for active products");

                        // Get current date in correct format
                        LocalDateTime now = LocalDateTime.now();
                        String formattedDate = now.format(DateTimeFormatter.ISO_DATE_TIME);
                        logger.info("Current date (formatted): {}", formattedDate);

                        // Step 1: Get all completed orders using raw search without model
                        // deserialization
                        String orderQueryJson = String.format(
                                        "{\"query\":{\"term\":{\"status.keyword\":\"COMPLETED\"}}}");

                        SearchRequest orderRequest = new SearchRequest.Builder()
                                        .index("orders")
                                        .withJson(new StringReader(orderQueryJson))
                                        .size(10000)
                                        .build();

                        // Use JsonData instead of OrderES class
                        SearchResponse<JsonData> orderResponse = elasticsearchClient.search(orderRequest,
                                        JsonData.class);
                        logger.info("Found {} completed orders",
                                        orderResponse.hits().total() != null ? orderResponse.hits().total().value()
                                                        : 0);

                        if (orderResponse.hits().total() == null || orderResponse.hits().total().value() == 0) {
                                logger.warn("No completed orders found");
                                return ResponseEntity.ok(result);
                        }

                        // Extract product IDs from orders
                        Set<String> productIds = new HashSet<>();
                        for (Hit<JsonData> hit : orderResponse.hits().hits()) {
                                JsonData orderData = hit.source();
                                if (orderData != null) {
                                        try {
                                                // JsonData approach avoids deserialization issues with date formats
                                                String productId = orderData.to(Map.class).get("productId").toString();
                                                logger.info("Found order for product: {}", productId);
                                                productIds.add(productId);
                                        } catch (Exception e) {
                                                logger.warn("Could not extract productId from order: {}",
                                                                e.getMessage());
                                        }
                                }
                        }

                        for (String productId : productIds) {
                                logger.info("Checking product: {}", productId);

                                // Direct JSON query for active configs
                                String configQueryJson = String.format(
                                                "{\"query\":{\"bool\":{\"must\":[" +
                                                                "{\"term\":{\"productId.keyword\":\"%s\"}}," +
                                                                "{\"term\":{\"enabled\":true}}," +
                                                                "{\"range\":{\"startDate\":{\"lte\":\"%s\"}}}," +
                                                                "{\"range\":{\"endDate\":{\"gte\":\"%s\"}}}" +
                                                                "]}}}",
                                                productId, formattedDate, formattedDate);

                                logger.info("Config query: {}", configQueryJson);

                                SearchRequest configRequest = new SearchRequest.Builder()
                                                .index("product_configs")
                                                .withJson(new StringReader(configQueryJson))
                                                .build();

                                // Use JsonData instead of ProductConfigES class
                                SearchResponse<JsonData> configResponse = elasticsearchClient.search(configRequest,
                                                JsonData.class);

                                long hits = configResponse.hits().total() != null
                                                ? configResponse.hits().total().value()
                                                : 0;
                                logger.info("Found {} active configs for product {}", hits, productId);

                                if (hits > 0) {
                                        result.add(productId);
                                }
                        }

                        // Also test for our test product
                        String testQueryJson = String.format(
                                        "{\"query\":{\"bool\":{\"must\":[" +
                                                        "{\"term\":{\"productId.keyword\":\"TEST-PROD-DIRECT\"}}," +
                                                        "{\"term\":{\"enabled\":true}}," +
                                                        "{\"range\":{\"startDate\":{\"lte\":\"%s\"}}}," +
                                                        "{\"range\":{\"endDate\":{\"gte\":\"%s\"}}}" +
                                                        "]}}}",
                                        formattedDate, formattedDate);

                        logger.info("Test product query: {}", testQueryJson);

                        SearchRequest testRequest = new SearchRequest.Builder()
                                        .index("product_configs")
                                        .withJson(new StringReader(testQueryJson))
                                        .build();

                        // Use JsonData instead of ProductConfigES class
                        SearchResponse<JsonData> testResponse = elasticsearchClient.search(testRequest, JsonData.class);

                        long testHits = testResponse.hits().total() != null ? testResponse.hits().total().value() : 0;
                        logger.info("Found {} active configs for test product", testHits);

                        if (testHits > 0) {
                                result.add("TEST-PROD-DIRECT");
                        }

                        logger.info("Final result: {} active products", result.size());
                        return ResponseEntity.ok(result);
                } catch (Exception e) {
                        logger.error("Error executing manual Elasticsearch query", e);
                        return ResponseEntity.ok(new ArrayList<>());
                }
        }
}