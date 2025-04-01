package com.example.demo.repository;

import com.example.demo.dto.ProductIdDTO;
import com.example.demo.model.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepositoryCustom {
        private final MongoTemplate mongoTemplate;

        public OrderRepositoryImpl(MongoTemplate mongoTemplate) {
                this.mongoTemplate = mongoTemplate;
        }

        @Override
        public List<Order> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
                Aggregation aggregation = Aggregation.newAggregation(
                                Aggregation.match(
                                                Criteria.where("createdAt").gte(startDate).andOperator(
                                                                Criteria.where("createdAt").lte(endDate))));

                AggregationResults<Order> results = mongoTemplate.aggregate(
                                aggregation,
                                "orders",
                                Order.class);

                return results.getMappedResults();
        }

        @Override
        public List<String> findDistinctActiveProducts() {
                LocalDateTime now = LocalDateTime.now();

                // Match orders with status "COMPLETED" first to reduce the dataset
                MatchOperation matchOrdersStage = Aggregation.match(
                                Criteria.where("status").is("COMPLETED"));

                // Lookup product configurations
                LookupOperation lookupStage = Aggregation.lookup(
                                "product_configs",
                                "productId",
                                "productId",
                                "productConfig"
                );

                // Unwind the productConfig array
                UnwindOperation unwindStage = Aggregation.unwind("productConfig", true);

                // Match active product configurations
                MatchOperation matchConfigsStage = Aggregation.match(
                                new Criteria().andOperator(
                                                Criteria.where("productConfig.enabled").is(true),
                                                Criteria.where("productConfig.startDate").lte(now),
                                                Criteria.where("productConfig.endDate").gt(now)
                                )
                );

                // Group by productId to get distinct values
                GroupOperation groupStage = Aggregation.group("productId");

                // Project final result
                ProjectionOperation projectStage = Aggregation.project()
                                .and("_id").as("productId");

                // Create the aggregation pipeline
                Aggregation aggregation = Aggregation.newAggregation(
                                matchOrdersStage,
                                lookupStage,
                                unwindStage,
                                matchConfigsStage,
                                groupStage,
                                projectStage
                );

                // Execute the aggregation with a longer timeout
                AggregationResults<ProductIdDTO> results = mongoTemplate.aggregate(
                                aggregation,
                                "orders",
                                ProductIdDTO.class);

                return results.getMappedResults().stream()
                                .map(ProductIdDTO::getProductId)
                                .toList();
        }
}