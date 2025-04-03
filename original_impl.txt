package com.example.demo.repository;

import com.example.demo.dto.ProductIdDTO;
import com.example.demo.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
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
@Primary
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

                logger.info("Starting optimized aggregation query with indexes for distinct active products at {}",
                                now);

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

                // Execute the aggregation pipeline with options including the hint
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
                long executeStartTime = System.currentTimeMillis();
                List<String> results = mongoTemplate.aggregate(
                                aggregation,
                                Order.class,
                                ProductIdDTO.class).getMappedResults().stream()
                                .map(ProductIdDTO::getProductId)
                                .toList();
                long executeEndTime = System.currentTimeMillis();

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;

                logger.info("Aggregation query with indexes executed in {} ms (MongoDB execution: {} ms)",
                                totalTime, (executeEndTime - executeStartTime));
                logger.info("Found {} distinct active products", results.size());

                return results;
        }

        // For comparison using the new indexes
        public List<String> findDistinctActiveProductsWithAggregation() {
                LocalDateTime now = LocalDateTime.now();
                long startTime = System.currentTimeMillis();

                logger.info("Starting optimized two-step query with indexes for distinct active products at {}", now);

                // Step 1: Get all completed orders with index support
                Query orderQuery = new Query(Criteria.where("status").is("COMPLETED"));
                orderQuery.fields().include("productId").exclude("_id");

                // Hint the query to use the status_productId compound index
                orderQuery.withHint("status_1_productId_1");

                long executeStep1StartTime = System.currentTimeMillis();
                Set<String> productIds = mongoTemplate.find(orderQuery, Order.class)
                                .stream()
                                .map(Order::getProductId)
                                .collect(Collectors.toSet());
                long executeStep1EndTime = System.currentTimeMillis();

                logger.info("Step 1 executed in {} ms, found {} product IDs",
                                (executeStep1EndTime - executeStep1StartTime), productIds.size());

                if (productIds.isEmpty()) {
                        logger.info("No completed orders found, returning empty list");
                        return List.of();
                }

                // Step 2: Filter active product configurations
                Query configQuery = new Query(Criteria.where("productId").in(productIds)
                                .and("enabled").is(true)
                                .and("startDate").lte(now)
                                .and("endDate").gte(now));

                // Hint the query to use appropriate indexes
                configQuery.withHint("enabled_1_startDate_1_endDate_1");
                configQuery.fields().include("productId").exclude("_id");

                long executeStep2StartTime = System.currentTimeMillis();
                List<String> results = mongoTemplate.find(configQuery, Document.class, "product_configs")
                                .stream()
                                .map(doc -> doc.getString("productId"))
                                .distinct()
                                .collect(Collectors.toList());
                long executeStep2EndTime = System.currentTimeMillis();

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                long executionTime = (executeStep1EndTime - executeStep1StartTime) +
                                (executeStep2EndTime - executeStep2StartTime);

                logger.info("Step 2 executed in {} ms, found {} active products",
                                (executeStep2EndTime - executeStep2StartTime), results.size());
                logger.info("Two-step query with indexes executed in {} ms (MongoDB execution: {} ms)",
                                totalTime, executionTime);
                logger.info("Found {} distinct active products", results.size());

                return results;
        }

        // Third optimized version using MongoDB's distinct operation and aggregation
        public List<String> findDistinctActiveProductsOptimized() {
                LocalDateTime now = LocalDateTime.now();
                long startTime = System.currentTimeMillis();

                logger.info("Starting optimized MongoDB-native distinct and aggregation query at {}", now);

                // Step 1: Use MongoDB's aggregate to get distinct productIds from completed
                // orders
                long executeStep1StartTime = System.currentTimeMillis();

                // Create optimized aggregation pipeline for distinct product IDs
                Aggregation distinctProductsAggregation = Aggregation.newAggregation(
                                // Match completed orders
                                Aggregation.match(Criteria.where("status").is("COMPLETED")),
                                // Group by productId to get distinct values
                                Aggregation.group("productId"),
                                // Project to include just the productId field
                                Aggregation.project().and("_id").as("productId")).withOptions(
                                                Aggregation.newAggregationOptions()
                                                                .allowDiskUse(true)
                                                                .hint(new Document("status", 1).append("productId", 1))
                                                                .build());

                List<ProductIdDTO> distinctProductIds = mongoTemplate.aggregate(
                                distinctProductsAggregation,
                                Order.class,
                                ProductIdDTO.class).getMappedResults();

                // Extract product IDs from the results
                List<String> productIds = distinctProductIds.stream()
                                .map(ProductIdDTO::getProductId)
                                .toList();

                long executeStep1EndTime = System.currentTimeMillis();
                logger.info("Step 1 optimized distinct operation executed in {} ms, found {} distinct product IDs",
                                (executeStep1EndTime - executeStep1StartTime), productIds.size());

                if (productIds.isEmpty()) {
                        logger.info("No completed orders found, returning empty list");
                        return List.of();
                }

                // Step 2: Use aggregation to find active product configurations
                long executeStep2StartTime = System.currentTimeMillis();

                // Create pipeline for active product configurations
                Aggregation activeProductsAggregation = Aggregation.newAggregation(
                                // Match product IDs from step 1 and active conditions
                                Aggregation.match(
                                                Criteria.where("productId").in(productIds)
                                                                .and("enabled").is(true)
                                                                .and("startDate").lte(now)
                                                                .and("endDate").gte(now)),
                                // Group by productId again to ensure distinctness
                                Aggregation.group("productId"),
                                // Project to include just the productId field
                                Aggregation.project().and("_id").as("productId")).withOptions(
                                                Aggregation.newAggregationOptions()
                                                                .allowDiskUse(true)
                                                                .hint(new Document("enabled", 1).append("startDate", 1)
                                                                                .append("endDate", 1))
                                                                .build());

                List<String> results = mongoTemplate.aggregate(
                                activeProductsAggregation,
                                "product_configs",
                                ProductIdDTO.class).getMappedResults().stream()
                                .map(ProductIdDTO::getProductId)
                                .toList();

                long executeStep2EndTime = System.currentTimeMillis();

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                long executionTime = (executeStep1EndTime - executeStep1StartTime) +
                                (executeStep2EndTime - executeStep2StartTime);

                logger.info("Step 2 optimized aggregation executed in {} ms, found {} active products",
                                (executeStep2EndTime - executeStep2StartTime), results.size());
                logger.info("MongoDB-native distinct and aggregation query executed in {} ms (MongoDB execution: {} ms)",
                                totalTime, executionTime);
                logger.info("Found {} distinct active products", results.size());

                return results;
        }
}