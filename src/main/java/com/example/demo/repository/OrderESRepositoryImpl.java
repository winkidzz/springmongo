package com.example.demo.repository;

import com.example.demo.model.OrderES;
import com.example.demo.model.ProductConfigES;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.io.StringReader;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class OrderESRepositoryImpl implements OrderESRepositoryCustom {

        private static final Logger logger = LoggerFactory.getLogger(OrderESRepositoryImpl.class);

        @Autowired
        private ElasticsearchClient elasticsearchClient;

        @Autowired
        private ProductConfigESRepository productConfigESRepository;

        @Override
        public List<String> findDistinctActiveProductsES() {
                try {
                        long startTime = System.currentTimeMillis();
                        logger.info("Starting Elasticsearch two-step query for distinct active products");

                        // Step 1: Get all completed orders
                        SearchRequest searchRequest = new SearchRequest.Builder()
                                        .index("orders")
                                        .query(q -> q
                                                        .term(t -> t
                                                                        .field("status.keyword")
                                                                        .value("COMPLETED")))
                                        .size(10000)
                                        .build();

                        logger.debug("Search request: {}", searchRequest.toString());
                        SearchResponse<OrderES> response = elasticsearchClient.search(searchRequest, OrderES.class);
                        logger.debug("Search response: {}", response.toString());
                        logger.info("Total hits: {}",
                                        response.hits().total() != null ? response.hits().total().value() : 0);

                        // Log a sample of the hits to verify document structure
                        if (!response.hits().hits().isEmpty()) {
                                Hit<OrderES> sampleHit = response.hits().hits().get(0);
                                logger.info("Sample hit source: {}", sampleHit.source());
                        }

                        // Extract distinct product IDs
                        Set<String> productIds = response.hits().hits().stream()
                                        .map(Hit::source)
                                        .filter(source -> source != null)
                                        .map(OrderES::getProductId)
                                        .collect(Collectors.toSet());

                        long step1EndTime = System.currentTimeMillis();
                        logger.info("Step 1: Found {} distinct product IDs in {} ms",
                                        productIds.size(), (step1EndTime - startTime));
                        logger.debug("Product IDs: {}", productIds);

                        if (productIds.isEmpty()) {
                                logger.warn("No product IDs found in completed orders. Returning empty list.");
                                return Collections.emptyList();
                        }

                        // Step 2: Get active product configurations for these IDs
                        LocalDateTime now = LocalDateTime.now();
                        logger.debug("Querying active products with now = {}", now);

                        // Use the custom method instead of Spring Data's method
                        List<ProductConfigES> activeConfigs = ((ProductConfigESRepositoryCustom) productConfigESRepository)
                                        .findActiveProductConfigsByProductIds(new ArrayList<>(productIds), now);

                        logger.debug("Found {} active configurations", activeConfigs.size());
                        for (ProductConfigES config : activeConfigs) {
                                logger.debug("Active config: id={}, productId={}, enabled={}, startDate={}, endDate={}",
                                                config.getId(), config.getProductId(), config.isEnabled(),
                                                config.getStartDate(), config.getEndDate());
                        }

                        List<String> results = activeConfigs.stream()
                                        .map(ProductConfigES::getProductId)
                                        .distinct()
                                        .collect(Collectors.toList());

                        long endTime = System.currentTimeMillis();
                        logger.info("Step 2: Filter for active configs completed in {} ms",
                                        (endTime - step1EndTime));
                        logger.info("Total execution time: {} ms, found {} active products",
                                        (endTime - startTime), results.size());
                        logger.debug("Active product IDs: {}", results);

                        return results;
                } catch (Exception e) {
                        logger.error("Error executing findDistinctActiveProductsES", e);
                        return Collections.emptyList();
                }
        }

        @Override
        public List<String> findDistinctActiveProductsESNative() {
                try {
                        long startTime = System.currentTimeMillis();
                        logger.info("Starting Elasticsearch native aggregation query for distinct active products");

                        // Step 1: Use Elasticsearch aggregation to get distinct productIds from
                        // completed orders
                        SearchRequest searchRequest = new SearchRequest.Builder()
                                        .index("orders")
                                        .query(q -> q
                                                        .term(t -> t
                                                                        .field("status.keyword")
                                                                        .value("COMPLETED")))
                                        .aggregations("distinct_product_ids", a -> a
                                                        .terms(t -> t
                                                                        .field("productId.keyword")
                                                                        .size(10000)))
                                        .size(0) // We only need aggregations, not hits
                                        .build();

                        logger.debug("Native search request: {}", searchRequest.toString());
                        SearchResponse<OrderES> response = elasticsearchClient.search(searchRequest, OrderES.class);
                        logger.debug("Native search response aggregations: {}",
                                        response.aggregations() != null ? "present" : "null");

                        // Extract the distinct product IDs from the aggregation result
                        List<String> distinctProductIds = Collections.emptyList();
                        if (response.aggregations() != null
                                        && response.aggregations().get("distinct_product_ids") != null) {
                                distinctProductIds = response.aggregations()
                                                .get("distinct_product_ids")
                                                .sterms()
                                                .buckets().array()
                                                .stream()
                                                .map(bucket -> bucket.key().stringValue())
                                                .collect(Collectors.toList());
                        }

                        long step1EndTime = System.currentTimeMillis();
                        logger.info("Step 1: Native ES aggregation completed in {} ms, found {} distinct products",
                                        (step1EndTime - startTime), distinctProductIds.size());
                        logger.debug("Distinct product IDs: {}", distinctProductIds);

                        if (distinctProductIds.isEmpty()) {
                                return Collections.emptyList();
                        }

                        // Step 2: Get active product configurations for these IDs
                        LocalDateTime now = LocalDateTime.now();
                        logger.debug("Querying active products with now = {}", now);

                        // Use the custom method instead of Spring Data's method
                        List<ProductConfigES> activeConfigs = ((ProductConfigESRepositoryCustom) productConfigESRepository)
                                        .findActiveProductConfigsByProductIds(distinctProductIds, now);

                        logger.debug("Found {} active configurations", activeConfigs.size());
                        for (ProductConfigES config : activeConfigs) {
                                logger.debug("Active config: id={}, productId={}, enabled={}, startDate={}, endDate={}",
                                                config.getId(), config.getProductId(), config.isEnabled(),
                                                config.getStartDate(), config.getEndDate());
                        }

                        List<String> results = activeConfigs.stream()
                                        .map(ProductConfigES::getProductId)
                                        .distinct()
                                        .collect(Collectors.toList());

                        long endTime = System.currentTimeMillis();
                        logger.info("Step 2: Filter for active configs completed in {} ms",
                                        (endTime - step1EndTime));
                        logger.info("Total execution time: {} ms, found {} active products",
                                        (endTime - startTime), results.size());
                        logger.debug("Active product IDs: {}", results);

                        return results;
                } catch (Exception e) {
                        logger.error("Error executing findDistinctActiveProductsESNative", e);
                        return Collections.emptyList();
                }
        }

        @Override
        public List<String> findDistinctActiveProductsOptimized() {
                try {
                        long startTime = System.currentTimeMillis();
                        logger.info("Starting optimized Elasticsearch query for active products");

                        // Use current date for active product check
                        LocalDateTime now = LocalDateTime.now();
                        String formattedDate = now.format(DateTimeFormatter.ISO_DATE_TIME);

                        // Single-query solution using aggregation and filter in one step
                        String rawQuery = String.format(
                                        "{" +
                                                        "  \"size\": 0," +
                                                        "  \"query\": {" +
                                                        "    \"term\": {" +
                                                        "      \"status.keyword\": \"COMPLETED\"" +
                                                        "    }" +
                                                        "  }," +
                                                        "  \"aggs\": {" +
                                                        "    \"distinct_products\": {" +
                                                        "      \"terms\": {" +
                                                        "        \"field\": \"productId.keyword\"," +
                                                        "        \"size\": 10000" +
                                                        "      }," +
                                                        "      \"aggs\": {" +
                                                        "        \"active_configs\": {" +
                                                        "          \"filter\": {" +
                                                        "            \"bool\": {" +
                                                        "              \"must\": [" +
                                                        "                { \"term\": { \"enabled\": true } }," +
                                                        "                { \"range\": { \"startDate\": { \"lte\": \"%s\" } } },"
                                                        +
                                                        "                { \"range\": { \"endDate\": { \"gte\": \"%s\" } } }"
                                                        +
                                                        "              ]" +
                                                        "            }" +
                                                        "          }" +
                                                        "        }" +
                                                        "      }" +
                                                        "    }" +
                                                        "  }" +
                                                        "}",
                                        formattedDate, formattedDate);

                        try {
                                // Execute the query
                                SearchRequest request = new SearchRequest.Builder()
                                                .index("orders")
                                                .withJson(new StringReader(rawQuery))
                                                .build();

                                SearchResponse<Void> response = elasticsearchClient.search(request, Void.class);

                                // Process results - extract active product IDs
                                List<String> activeProductIds = new ArrayList<>();

                                if (response.aggregations() != null) {
                                        var productBuckets = response.aggregations()
                                                        .get("distinct_products")
                                                        .sterms()
                                                        .buckets().array();

                                        for (var bucket : productBuckets) {
                                                String productId = bucket.key().stringValue();
                                                // Only include products that have active configurations
                                                var activeConfigAgg = bucket.aggregations().get("active_configs");
                                                if (activeConfigAgg != null
                                                                && activeConfigAgg.filter().docCount() > 0) {
                                                        activeProductIds.add(productId);
                                                }
                                        }
                                }

                                long duration = System.currentTimeMillis() - startTime;
                                logger.info("Optimized query completed in {} ms, found {} active products",
                                                duration, activeProductIds.size());

                                return activeProductIds;

                        } catch (IOException e) {
                                logger.error("Error executing optimized Elasticsearch query", e);
                                return Collections.emptyList();
                        }

                } catch (Exception e) {
                        logger.error("Error in findDistinctActiveProductsOptimized", e);
                        return Collections.emptyList();
                }
        }
}