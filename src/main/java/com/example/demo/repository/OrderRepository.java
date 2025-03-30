package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import com.example.demo.dto.OrderSummaryDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface OrderRepository extends MongoRepository<Order, String>, OrderRepositoryCustom {

        Logger logger = LoggerFactory.getLogger(OrderRepository.class);

        @Aggregation(pipeline = {
                        "{ $match: { " +
                                        "   status: { $in: ['PENDING', 'PROCESSING', 'CANCELLED'] }, " +
                                        "   price: { $gt: 10 }, " +
                                        "   orderDate: { $gte: ?0 } " +
                                        "} }",
                        "{ $lookup: { " +
                                        "   from: 'product_configs', " +
                                        "   localField: 'productId', " +
                                        "   foreignField: 'productId', " +
                                        "   as: 'productConfig' " +
                                        "} }",
                        "{ $unwind: '$productConfig' }",
                        "{ $match: { " +
                                        "   'productConfig.enabled': true, " +
                                        "   'productConfig.startDate': { $gte: ?0 }, " +
                                        "   'productConfig.endDate': { $lte: ?1 } " +
                                        "} }",
                        "{ $group: { " +
                                        "   _id: '$productName', " +
                                        "   totalOrders: { $sum: 1 }, " +
                                        "   totalQuantity: { $sum: '$quantity' }, " +
                                        "   totalPrice: { $sum: { $multiply: ['$price', '$quantity'] } }, " +
                                        "   averagePrice: { $avg: '$price' }, " +
                                        "   statusCounts: { " +
                                        "       $push: { " +
                                        "           status: '$status', " +
                                        "           count: 1 " +
                                        "       } " +
                                        "   } " +
                                        "} }",
                        "{ $project: { " +
                                        "   _id: 0, " +
                                        "   productName: '$_id', " +
                                        "   totalOrders: 1, " +
                                        "   totalQuantity: 1, " +
                                        "   totalPrice: 1, " +
                                        "   averagePrice: 1, " +
                                        "   statusBreakdown: { " +
                                        "       $map: { " +
                                        "           input: '$statusCounts', " +
                                        "           as: 'status', " +
                                        "           in: { " +
                                        "               status: '$$status.status', " +
                                        "               count: '$$status.count' " +
                                        "           } " +
                                        "       } " +
                                        "   } " +
                                        "} }"
        })
        List<OrderSummaryDTO> findOrderSummaries(LocalDateTime startDate, LocalDateTime endDate);
}