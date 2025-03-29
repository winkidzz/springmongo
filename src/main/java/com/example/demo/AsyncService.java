package com.example.demo;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class AsyncService {

    @Async("taskExecutor")
    public CompletableFuture<String> asyncMethod() {
        try {
            // Simulate a long-running task
            Thread.sleep(2000);
            return CompletableFuture.completedFuture("Async task completed!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }
} 