package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.Document;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {
        private static final Logger logger = LoggerFactory.getLogger(OrderRepositoryImpl.class);

        @Autowired
        private MongoTemplate mongoTemplate;

        @Override
        public List<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
                Aggregation aggregation = Aggregation.newAggregation(
                                Aggregation.match(
                                                Criteria.where("createdAt").gte(startDate)
                                                                .andOperator(Criteria.where("createdAt")
                                                                                .lte(endDate))));

                return mongoTemplate.aggregate(
                                aggregation,
                                Order.class,
                                Order.class).getMappedResults();
        }

        @Override
        public List<String> findDistinctActiveProducts() {
                LocalDateTime now = LocalDateTime.now();
                long startTime = System.currentTimeMillis();

                logger.info("Starting aggregation query for distinct active products at {}", now);

                // Match stage for completed orders with compound index support
                MatchOperation matchOrders = Aggregation.match(
                                Criteria.where("status").is("COMPLETED"));

                // Lookup stage for product configurations
                LookupOperation lookupProductConfigs = Aggregation.lookup()
                                .from("product_configs")
                                .localField("productId")
                                .foreignField("productId")
                                .as("productConfig");

                // Unwind the product config array
                UnwindOperation unwindProductConfig = Aggregation.unwind("productConfig");

                // Match stage for active product configurations
                MatchOperation matchActiveConfigs = Aggregation.match(
                                Criteria.where("productConfig.enabled").is(true)
                                                .and("productConfig.startDate").lte(now)
                                                .and("productConfig.endDate").gte(now));

                // Group by productId to get distinct values
                GroupOperation groupByProductId = Aggregation.group("productId");

                // Project stage to format the output
                ProjectionOperation project = Aggregation.project()
                                .and("_id").as("productId");

                // Execute the aggregation pipeline
                Aggregation aggregation = Aggregation.newAggregation(
                                matchOrders,
                                lookupProductConfigs,
                                unwindProductConfig,
                                matchActiveConfigs,
                                groupByProductId,
                                project).withOptions(
                                                Aggregation.newAggregationOptions()
                                                                .allowDiskUse(true)
                                                                .build());

                // Execute the query
                AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(
                                aggregation,
                                Order.class,
                                Document.class);

                List<String> results = aggregationResults.getMappedResults().stream()
                                .map(doc -> doc.getString("productId"))
                                .collect(Collectors.toList());

                long endTime = System.currentTimeMillis();
                logger.info("Aggregation query executed in {} ms", (endTime - startTime));
                logger.info("Found {} distinct active products", results.size());

                return results;
        }

        // Simplified implementation of the optimized method
        public List<String> findDistinctActiveProductsOptimized() {
                logger.info("Starting optimized MongoDB-native distinct and aggregation query");

                // Step 1: Get all completed orders with index support
                Query orderQuery = new Query(Criteria.where("status").is("COMPLETED"));
                orderQuery.fields().include("productId").exclude("_id");

                Set<String> productIds = mongoTemplate.find(orderQuery, Order.class)
                                .stream()
                                .map(Order::getProductId)
                                .collect(Collectors.toSet());

                logger.info("Found {} distinct product IDs from completed orders", productIds.size());

                if (productIds.isEmpty()) {
                        return List.of();
                }

                // Step 2: Filter for active product configurations
                LocalDateTime now = LocalDateTime.now();
                Query configQuery = new Query(Criteria.where("productId").in(productIds)
                                .and("enabled").is(true)
                                .and("startDate").lte(now)
                                .and("endDate").gte(now));

                List<Document> activeConfigs = mongoTemplate.find(configQuery, Document.class, "product_configs");

                List<String> results = activeConfigs.stream()
                                .map(doc -> doc.getString("productId"))
                                .distinct()
                                .collect(Collectors.toList());

                logger.info("Found {} active products", results.size());

                return results;
        }

        // New optimization using MongoDB's distinct operation directly
        public List<String> findDistinctActiveProductsWithMongoDistinct() {
                long startTime = System.currentTimeMillis();
                logger.info("Starting optimized query using MongoDB distinct operation");

                // Step 1: Get distinct productIds from completed orders directly using
                // MongoDB's distinct
                List<String> distinctProductIds = mongoTemplate.getCollection("orders")
                                .distinct("productId", new Document("status", "COMPLETED"), String.class)
                                .into(new java.util.ArrayList<>());

                long step1EndTime = System.currentTimeMillis();
                logger.info("Step 1: MongoDB distinct operation completed in {} ms, found {} distinct products",
                                (step1EndTime - startTime), distinctProductIds.size());

                if (distinctProductIds.isEmpty()) {
                        return List.of();
                }

                // Step 2: Filter for active product configurations
                LocalDateTime now = LocalDateTime.now();
                Query configQuery = new Query(Criteria.where("productId").in(distinctProductIds)
                                .and("enabled").is(true)
                                .and("startDate").lte(now)
                                .and("endDate").gte(now));

                List<Document> activeConfigs = mongoTemplate.find(configQuery, Document.class, "product_configs");

                List<String> results = activeConfigs.stream()
                                .map(doc -> doc.getString("productId"))
                                .distinct()
                                .collect(Collectors.toList());

                long endTime = System.currentTimeMillis();
                logger.info("Step 2: Filter for active configs completed in {} ms", (endTime - step1EndTime));
                logger.info("Total execution time: {} ms, found {} active products", (endTime - startTime),
                                results.size());

                return results;
        }

        // Implementation using proper index hints
        public List<String> findDistinctActiveProductsWithHint() {
                LocalDateTime now = LocalDateTime.now();
                long startTime = System.currentTimeMillis();

                logger.info("Starting aggregation query with proper index hint for active products at {}", now);

                // Create the proper hint document for the orders collection
                Document orderHint = new Document("status", 1).append("productId", 1);

                // Match stage for completed orders with compound index support
                MatchOperation matchOrders = Aggregation.match(
                                Criteria.where("status").is("COMPLETED"));

                // Lookup stage for product configurations
                LookupOperation lookupProductConfigs = Aggregation.lookup()
                                .from("product_configs")
                                .localField("productId")
                                .foreignField("productId")
                                .as("productConfig");

                // Unwind the product config array
                UnwindOperation unwindProductConfig = Aggregation.unwind("productConfig");

                // Match stage for active product configurations
                MatchOperation matchActiveConfigs = Aggregation.match(
                                Criteria.where("productConfig.enabled").is(true)
                                                .and("productConfig.startDate").lte(now)
                                                .and("productConfig.endDate").gte(now));

                // Group by productId to get distinct values
                GroupOperation groupByProductId = Aggregation.group("productId");

                // Project stage to format the output
                ProjectionOperation project = Aggregation.project()
                                .and("_id").as("productId");

                // Execute the aggregation pipeline with hint option
                Aggregation aggregation = Aggregation.newAggregation(
                                matchOrders,
                                lookupProductConfigs,
                                unwindProductConfig,
                                matchActiveConfigs,
                                groupByProductId,
                                project).withOptions(
                                                Aggregation.newAggregationOptions()
                                                                .allowDiskUse(true)
                                                                .hint(orderHint)
                                                                .build());

                // Execute the query
                AggregationResults<Document> aggregationResults = mongoTemplate.aggregate(
                                aggregation,
                                Order.class,
                                Document.class);

                List<String> results = aggregationResults.getMappedResults().stream()
                                .map(doc -> doc.getString("productId"))
                                .collect(Collectors.toList());

                long endTime = System.currentTimeMillis();
                logger.info("Aggregation query with hint executed in {} ms", (endTime - startTime));
                logger.info("Found {} distinct active products", results.size());

                return results;
        }
}