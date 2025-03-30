package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderSummaryDTO {
    private String productName;
    private long totalOrders;
    private int totalQuantity;
    private double totalPrice;
    private double averagePrice;
    private List<StatusCount> statusBreakdown;

    @Data
    public static class StatusCount {
        private String status;
        private long count;
    }
}