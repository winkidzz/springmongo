package com.example.demo.controller;

import com.example.demo.util.PerformanceTestUtil;
import com.example.demo.util.PerformanceTestUtil.TestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for running performance tests across different database
 * implementations
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceTestController {

    private final PerformanceTestUtil performanceTestUtil;

    /**
     * Run comprehensive performance tests across all database implementations
     * 
     * @param iterations Number of times to run each test (default: 5)
     * @param concurrent Number of concurrent users to simulate (default: 10)
     * @return Map of test results
     */
    @GetMapping("/test")
    public Map<String, TestResult> runPerformanceTests(
            @RequestParam(defaultValue = "5") int iterations,
            @RequestParam(defaultValue = "10") int concurrent) {

        log.info("Received request to run performance tests with {} iterations and {} concurrent users",
                iterations, concurrent);

        return performanceTestUtil.runComprehensiveTests(iterations, concurrent);
    }

    /**
     * Warmup endpoint to initialize connections and caches before testing
     */
    @PostMapping("/warmup")
    public String warmupSystems() {
        log.info("Warming up database connections and caches");

        // Run a single iteration with minimal concurrency to warm up connections
        performanceTestUtil.runComprehensiveTests(1, 1);

        return "Systems warmed up and ready for performance testing";
    }
}