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

                logger.info("Starting optimized aggregation query for distinct active products at {}", now);

                // APPROACH 1: Two-step query with memory caching
                // Step 1: Get all active product configurations first (this should be very fast
                // with proper indexing)
                Query activeConfigQuery = new Query(
                                Criteria.where("enabled").is(true)
                                                .and("startDate").lte(now)
                                                .and("endDate").gte(now));

                activeConfigQuery.fields().include("productId");

                // This query should use the compound index on product_configs
                Document activeConfigHint = new Document();
                activeConfigHint.put("enabled", 1);
                activeConfigHint.put("startDate", 1);
                activeConfigHint.put("endDate", 1);
                activeConfigQuery.withHint(activeConfigHint);

                long configStartTime = System.currentTimeMillis();
                List<String> activeProductIds = mongoTemplate.find(activeConfigQuery, Document.class, "product_configs")
                                .stream()
                                .map(doc -> doc.getString("productId"))
                                .collect(Collectors.toList());
                long configEndTime = System.currentTimeMillis();

                logger.info("Found {} active product configurations in {} ms",
                                activeProductIds.size(), (configEndTime - configStartTime));

                if (activeProductIds.isEmpty()) {
                        logger.info("No active product configurations found, returning empty list");
                        return List.of();
                }

                // Step 2: Find orders with these product IDs and status COMPLETED
                Query completedOrdersQuery = new Query(
                                Criteria.where("status").is("COMPLETED")
                                                .and("productId").in(activeProductIds));
                completedOrdersQuery.fields().include("productId");

                // This query should use the compound index on orders
                Document orderHint = new Document();
                orderHint.put("status", 1);
                orderHint.put("productId", 1);
                completedOrdersQuery.withHint(orderHint);

                long orderStartTime = System.currentTimeMillis();
                Set<String> distinctProductIds = mongoTemplate.find(completedOrdersQuery, Document.class, "orders")
                                .stream()
                                .map(doc -> doc.getString("productId"))
                                .collect(Collectors.toSet());
                long orderEndTime = System.currentTimeMillis();

                logger.info("Found {} distinct product IDs from completed orders in {} ms",
                                distinctProductIds.size(), (orderEndTime - orderStartTime));

                List<String> result = distinctProductIds.stream().toList();

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;

                logger.info("Optimized query executed in {} ms (config lookup: {} ms, order lookup: {} ms)",
                                totalTime,
                                (configEndTime - configStartTime),
                                (orderEndTime - orderStartTime));
                logger.info("Found {} distinct active products", result.size());

                return result;
        }

        // Keep the original method for comparison
        public List<String> findDistinctActiveProductsWithAggregation() {
                LocalDateTime now = LocalDateTime.now();
                long startTime = System.currentTimeMillis();

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

                // Add a hint to use the compound index
                Document hint = new Document();
                hint.put("status", 1);
                hint.put("productId", 1);

                // Execute the aggregation pipeline with options
                Aggregation aggregation = Aggregation.newAggregation(
                                matchOrders,
                                lookupProductConfigs,
                                unwindProductConfig,
                                matchActiveConfigs,
                                groupByProductId,
                                project)
                                .withOptions(
                                                Aggregation.newAggregationOptions()
                                                                .allowDiskUse(true)
                                                                .hint(hint)
                                                                .build());

                List<String> results = mongoTemplate.aggregate(
                                aggregation,
                                Order.class,
                                ProductIdDTO.class).getMappedResults().stream()
                                .map(ProductIdDTO::getProductId)
                                .toList();

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;

                logger.info("Aggregation pipeline query executed in {} ms", totalTime);
                logger.info("Found {} distinct active products", results.size());

                return results;
        }
}