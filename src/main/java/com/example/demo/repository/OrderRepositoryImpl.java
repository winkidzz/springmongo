package com.example.demo.repository;

import com.example.demo.dto.OrderSummaryDTO;
import com.example.demo.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {

        private static final Logger logger = LoggerFactory.getLogger(OrderRepositoryImpl.class);

        @Autowired
        private MongoTemplate mongoTemplate;

        @Override
        public List<OrderSummaryDTO> findOrderSummariesWithMetrics(LocalDateTime startDate, LocalDateTime endDate) {
                long startTime = System.currentTimeMillis();
                logger.info("Starting aggregation query with date range: {} to {}", startDate, endDate);

                // Match orders stage
                MatchOperation matchOrders = Aggregation.match(
                                Criteria.where("status").in("PENDING", "PROCESSING", "CANCELLED")
                                                .and("price").gt(10)
                                                .and("orderDate").gte(startDate));
                logger.debug("Match orders criteria: status in [PENDING, PROCESSING, CANCELLED], price > 10, orderDate >= {}",
                                startDate);

                // Lookup stage
                LookupOperation lookupConfigs = LookupOperation.newLookup()
                                .from("product_configs")
                                .localField("productId")
                                .foreignField("productId")
                                .as("productConfig");
                logger.debug("Lookup stage: joining with product_configs collection");

                // Unwind stage
                UnwindOperation unwindConfigs = Aggregation.unwind("productConfig");
                logger.debug("Unwind stage: flattening productConfig array");

                // Match configs stage
                MatchOperation matchConfigs = Aggregation.match(
                                Criteria.where("productConfig.enabled").is(true)
                                                .and("productConfig.startDate").gte(startDate)
                                                .and("productConfig.endDate").lte(endDate));
                logger.debug("Match configs criteria: enabled=true, startDate >= {}, endDate <= {}", startDate,
                                endDate);

                // Group stage
                GroupOperation groupByProduct = Aggregation.group("productName")
                                .count().as("totalOrders")
                                .sum("quantity").as("totalQuantity")
                                .sum(ArithmeticOperators.Multiply.valueOf("price").multiplyBy("quantity"))
                                .as("totalPrice")
                                .avg("price").as("averagePrice")
                                .push("status").as("statusCounts");
                logger.debug("Group stage: aggregating by productName with counts and calculations");

                // Project stage
                ProjectionOperation projectResults = Aggregation.project()
                                .and("_id").as("productName")
                                .and("totalOrders").as("totalOrders")
                                .and("totalQuantity").as("totalQuantity")
                                .and("totalPrice").as("totalPrice")
                                .and("averagePrice").as("averagePrice")
                                .and("statusCounts").as("statusBreakdown");
                logger.debug("Project stage: finalizing output fields");

                // Execute aggregation
                Aggregation aggregation = Aggregation.newAggregation(
                                matchOrders,
                                lookupConfigs,
                                unwindConfigs,
                                matchConfigs,
                                groupByProduct,
                                projectResults);

                logger.info("Executing aggregation pipeline...");
                logger.debug("Aggregation pipeline stages: {}", aggregation.getPipeline());

                AggregationResults<OrderSummaryDTO> results = mongoTemplate.aggregate(
                                aggregation,
                                Order.class,
                                OrderSummaryDTO.class);

                List<OrderSummaryDTO> summaries = results.getMappedResults();
                long endTime = System.currentTimeMillis();
                long executionTime = endTime - startTime;

                // Log performance metrics
                logger.info("Aggregation performance metrics:");
                logger.info("Total execution time: {} ms", executionTime);
                logger.info("Number of results: {}", summaries.size());
                logger.info("Average time per result: {} ms",
                                summaries.isEmpty() ? 0 : (double) executionTime / summaries.size());

                // Log detailed results for debugging
                if (logger.isDebugEnabled()) {
                        logger.debug("Detailed results:");
                        summaries.forEach(summary -> logger.debug("Product: {}, Orders: {}, Quantity: {}, Price: {}",
                                        summary.getProductName(),
                                        summary.getTotalOrders(),
                                        summary.getTotalQuantity(),
                                        summary.getTotalPrice()));
                }

                return summaries;
        }
}