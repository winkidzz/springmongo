package com.example.demo.util;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class DataGenerator {
    private final OrderRepository orderRepository;
    private static final Random RANDOM = new Random();

    @Autowired
    public DataGenerator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void generateTestData(int numOrders) {
        orderRepository.deleteAll();
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < numOrders; i++) {
            Order order = new Order();
            order.setProductId("PROD-" + (RANDOM.nextInt(100) + 1));
            order.setCustomerId("CUST-" + (RANDOM.nextInt(1000) + 1));
            order.setAmount(RANDOM.nextDouble() * 1000);
            order.setStatus(getRandomStatus());
            order.setCreatedAt(LocalDateTime.now().minusDays(RANDOM.nextInt(30)));
            order.setUpdatedAt(LocalDateTime.now());
            orders.add(order);
        }

        orderRepository.saveAll(orders);
    }

    private String getRandomStatus() {
        String[] statuses = { "PENDING", "PROCESSING", "COMPLETED", "CANCELLED" };
        return statuses[RANDOM.nextInt(statuses.length)];
    }
}