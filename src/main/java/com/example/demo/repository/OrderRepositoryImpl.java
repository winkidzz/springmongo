package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
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
}