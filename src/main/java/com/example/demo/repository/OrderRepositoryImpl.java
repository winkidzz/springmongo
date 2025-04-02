package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
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
                logger.info("Finding distinct active products");
                // Simplified implementation - just return an empty list for now
                return List.of();
        }
}