package com.example.demo.repository;

import com.example.demo.dto.ProductIdDTO;
import com.example.demo.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

                // Track execution time for each stage
                long stageStartTime;
                long matchOrdersTime = 0;
                long lookupTime = 0;
                long unwindTime = 0;
                long matchConfigsTime = 0;
                long groupTime = 0;
                long projectTime = 0;
                long executeTime = 0;

                // Match stage for completed orders with compound index support
                stageStartTime = System.currentTimeMillis();
                MatchOperation matchOrders = Aggregation.match(
                                Criteria.where("status").is("COMPLETED"));
                matchOrdersTime = System.currentTimeMillis() - stageStartTime;

                // Lookup stage for product configurations
                stageStartTime = System.currentTimeMillis();
                LookupOperation lookupProductConfigs = Aggregation.lookup()
                                .from("product_configs")
                                .localField("productId")
                                .foreignField("productId")
                                .as("productConfig");
                lookupTime = System.currentTimeMillis() - stageStartTime;

                // Unwind the product config array
                stageStartTime = System.currentTimeMillis();
                UnwindOperation unwindProductConfig = Aggregation.unwind("productConfig");
                unwindTime = System.currentTimeMillis() - stageStartTime;

                // Match stage for active product configurations
                stageStartTime = System.currentTimeMillis();
                MatchOperation matchActiveConfigs = Aggregation.match(
                                Criteria.where("productConfig.enabled").is(true)
                                                .and("productConfig.startDate").lte(now)
                                                .and("productConfig.endDate").gte(now));
                matchConfigsTime = System.currentTimeMillis() - stageStartTime;

                // Group by productId to get distinct values
                stageStartTime = System.currentTimeMillis();
                GroupOperation groupByProductId = Aggregation.group("productId");
                groupTime = System.currentTimeMillis() - stageStartTime;

                // Project stage to format the output
                stageStartTime = System.currentTimeMillis();
                ProjectionOperation project = Aggregation.project()
                                .and("_id").as("productId");
                projectTime = System.currentTimeMillis() - stageStartTime;

                // Execute the aggregation pipeline with options
                Aggregation aggregation = Aggregation.newAggregation(
                                matchOrders,
                                lookupProductConfigs,
                                unwindProductConfig,
                                matchActiveConfigs,
                                groupByProductId,
                                project).withOptions(
                                                Aggregation.newAggregationOptions()
                                                                .allowDiskUse(true) // Allow disk usage for large
                                                                                    // datasets
                                                                .build());

                logger.info("Aggregation pipeline built in {} ms",
                                matchOrdersTime + lookupTime + unwindTime + matchConfigsTime + groupTime + projectTime);
                logger.info("Stage timing: match={} ms, lookup={} ms, unwind={} ms, matchConfig={} ms, group={} ms, project={} ms",
                                matchOrdersTime, lookupTime, unwindTime, matchConfigsTime, groupTime, projectTime);

                // Execute the query
                stageStartTime = System.currentTimeMillis();
                List<String> results = mongoTemplate.aggregate(
                                aggregation,
                                Order.class,
                                ProductIdDTO.class).getMappedResults().stream()
                                .map(ProductIdDTO::getProductId)
                                .toList();
                executeTime = System.currentTimeMillis() - stageStartTime;

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;

                logger.info("Aggregation query executed in {} ms (MongoDB execution: {} ms)", totalTime, executeTime);
                logger.info("Found {} distinct active products", results.size());
                logger.info("Average time per result: {} ms", results.isEmpty() ? 0 : totalTime / results.size());

                return results;
        }
}