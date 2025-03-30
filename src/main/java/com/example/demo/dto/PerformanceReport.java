package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PerformanceReport {
    private List<IterationResult> iterations;
    private Summary summary;

    @Data
    public static class IterationResult {
        private int iterationNumber;
        private long executionTime;
        private int resultCount;
        private List<OrderSummaryDTO> results;
    }

    @Data
    public static class Summary {
        private double averageExecutionTime;
        private long minExecutionTime;
        private long maxExecutionTime;
        private double averageResults;
        private int totalIterations;
        private long totalExecutionTime;
    }
}