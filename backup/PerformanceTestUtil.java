package com.example.demo.util;

import com.example.demo.controller.ActiveProductApiController;
import com.example.demo.controller.ActiveOrderController;
import com.example.demo.controller.OrderController;
import com.example.demo.controller.ElasticsearchController;
import com.example.demo.controller.RedisCachedController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Utility for running performance tests against various database
 * implementations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceTestUtil {

        private final OrderController orderController;
        private final ElasticsearchController elasticsearchController;
        private final RedisCachedController redisCachedController;
        private final ActiveProductApiController activeProductApiController;
        private final ActiveOrderController activeOrderController;

        /**
         * Run performance tests for all implemented DB configurations
         * 
         * @param iterations Number of times to run each test
         * @param concurrent Number of concurrent requests to simulate
         * @return Map containing test results
         */
        public Map<String, TestResult> runComprehensiveTests(int iterations, int concurrent) {
                Map<String, TestResult> results = new HashMap<>();

                log.info("Starting comprehensive performance tests with {} iterations and {} concurrent users",
                                iterations, concurrent);

                // Test MongoDB direct access
                results.put("mongodb_direct",
                                runTest("MongoDB Direct", iterations, concurrent,
                                                () -> orderController.getActiveProducts()));

                // Test Elasticsearch optimized
                results.put("elasticsearch_optimized",
                                runTest("Elasticsearch Optimized", iterations, concurrent,
                                                () -> elasticsearchController.getActiveProductsOptimized()));

                // Test Redis cached Elasticsearch
                results.put("redis_cached_elasticsearch",
                                runTest("Redis-Cached Elasticsearch", iterations, concurrent,
                                                () -> redisCachedController
                                                                .getElasticsearchActiveProductsWithRedisCache()));

                // Test Redis-MongoDB dual-write
                results.put("redis_mongodb_dualwrite",
                                runTest("Redis-MongoDB Dual-Write", iterations, concurrent,
                                                () -> activeProductApiController.getActiveProductIds()));

                // Test order retrieval with active products
                results.put("orders_for_active_products",
                                runTest("Orders for Active Products", iterations, concurrent,
                                                () -> activeOrderController.getOrdersForActiveProducts()));

                log.info("Performance tests completed");
                printResults(results);

                return results;
        }

        /**
         * Run a single test multiple times and record performance metrics
         */
        private TestResult runTest(String name, int iterations, int concurrentUsers, Runnable testOperation) {
                log.info("Starting test: {}", name);

                List<Long> executionTimes = new ArrayList<>();
                List<Long> concurrentExecutionTimes = new ArrayList<>();

                // Single-threaded test
                for (int i = 0; i < iterations; i++) {
                        long start = System.currentTimeMillis();
                        testOperation.run();
                        long end = System.currentTimeMillis();
                        executionTimes.add(end - start);
                }

                // Concurrent test
                ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
                try {
                        for (int i = 0; i < iterations; i++) {
                                long start = System.currentTimeMillis();
                                List<Future<?>> futures = new ArrayList<>();

                                for (int j = 0; j < concurrentUsers; j++) {
                                        futures.add(executor.submit(testOperation));
                                }

                                // Wait for all tasks to complete
                                for (Future<?> future : futures) {
                                        future.get();
                                }

                                long end = System.currentTimeMillis();
                                concurrentExecutionTimes.add(end - start);
                        }
                } catch (Exception e) {
                        log.error("Error during concurrent test execution", e);
                } finally {
                        executor.shutdown();
                        try {
                                executor.awaitTermination(30, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                                log.error("Executor service termination interrupted", e);
                        }
                }

                // Calculate metrics
                double avgTime = executionTimes.stream()
                                .mapToLong(Long::longValue)
                                .average()
                                .orElse(0);

                double avgConcurrentTime = concurrentExecutionTimes.stream()
                                .mapToLong(Long::longValue)
                                .average()
                                .orElse(0);

                long minTime = executionTimes.stream()
                                .mapToLong(Long::longValue)
                                .min()
                                .orElse(0);

                long maxTime = executionTimes.stream()
                                .mapToLong(Long::longValue)
                                .max()
                                .orElse(0);

                TestResult result = new TestResult(
                                name,
                                avgTime,
                                avgConcurrentTime,
                                minTime,
                                maxTime,
                                iterations,
                                concurrentUsers);

                log.info("Test completed: {}. Avg time: {} ms, Concurrent avg time: {} ms",
                                name, avgTime, avgConcurrentTime);

                return result;
        }

        /**
         * Print a summary of test results
         */
        private void printResults(Map<String, TestResult> results) {
                log.info("\n----- PERFORMANCE TEST RESULTS -----");
                log.info(String.format("%-30s %-15s %-20s %-15s %-15s",
                                "Implementation", "Avg Time (ms)", "Concurrent Avg (ms)", "Min Time (ms)",
                                "Max Time (ms)"));

                results.values().forEach(result -> {
                        log.info(String.format("%-30s %-15.2f %-20.2f %-15d %-15d",
                                        result.name(),
                                        result.averageTime(),
                                        result.concurrentAverageTime(),
                                        result.minTime(),
                                        result.maxTime()));
                });

                log.info("-------------------------------------");
        }

        /**
         * Record class for test results
         */
        public record TestResult(
                        String name,
                        double averageTime,
                        double concurrentAverageTime,
                        long minTime,
                        long maxTime,
                        int iterations,
                        int concurrentUsers) {
        }
}