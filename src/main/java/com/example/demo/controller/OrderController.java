package com.example.demo.controller;

import com.example.demo.dto.OrderSummaryDTO;
import com.example.demo.dto.PerformanceReport;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/summary")
    public ResponseEntity<PerformanceReport> getOrderSummaries() {
        int iterations = 10;
        List<PerformanceReport.IterationResult> iterationResults = new ArrayList<>();
        long totalExecutionTime = 0;

        System.out.println("Starting performance test with " + iterations + " iterations...");

        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            List<OrderSummaryDTO> summaries = orderService.getOrderSummaries();
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            totalExecutionTime += executionTime;

            PerformanceReport.IterationResult result = new PerformanceReport.IterationResult();
            result.setIterationNumber(i + 1);
            result.setExecutionTime(executionTime);
            result.setResultCount(summaries.size());
            result.setResults(summaries);
            iterationResults.add(result);

            System.out.println("Iteration " + (i + 1) + ":");
            System.out.println("  Execution time: " + executionTime + "ms");
            System.out.println("  Number of results: " + summaries.size());
        }

        // Calculate summary statistics
        PerformanceReport.Summary summary = new PerformanceReport.Summary();
        summary.setAverageExecutionTime(iterationResults.stream()
                .mapToLong(PerformanceReport.IterationResult::getExecutionTime)
                .average()
                .orElse(0.0));
        summary.setMinExecutionTime(iterationResults.stream()
                .mapToLong(PerformanceReport.IterationResult::getExecutionTime)
                .min()
                .orElse(0));
        summary.setMaxExecutionTime(iterationResults.stream()
                .mapToLong(PerformanceReport.IterationResult::getExecutionTime)
                .max()
                .orElse(0));
        summary.setAverageResults(iterationResults.stream()
                .mapToInt(PerformanceReport.IterationResult::getResultCount)
                .average()
                .orElse(0.0));
        summary.setTotalIterations(iterations);
        summary.setTotalExecutionTime(totalExecutionTime);

        System.out.println("\nPerformance Summary:");
        System.out
                .println("Average execution time: " + String.format("%.2f", summary.getAverageExecutionTime()) + "ms");
        System.out.println("Minimum execution time: " + summary.getMinExecutionTime() + "ms");
        System.out.println("Maximum execution time: " + summary.getMaxExecutionTime() + "ms");
        System.out.println("Average number of results: " + String.format("%.2f", summary.getAverageResults()));
        System.out.println("Total execution time: " + summary.getTotalExecutionTime() + "ms");

        PerformanceReport report = new PerformanceReport();
        report.setIterations(iterationResults);
        report.setSummary(summary);

        return ResponseEntity.ok(report);
    }
}