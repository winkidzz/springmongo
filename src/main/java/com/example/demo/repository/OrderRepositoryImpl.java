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

                List<String> results = mongoTemplate.aggregate(
                                aggregation,
                                Order.class,
                                ProductIdDTO.class).getMappedResults().stream()
                                .map(ProductIdDTO::getProductId)
                                .toList();

                long endTime = System.currentTimeMillis();
                logger.info("Aggregation query executed in {} ms", (endTime - startTime));
                logger.info("Found {} distinct active products", results.size());

                return results;
        }
}