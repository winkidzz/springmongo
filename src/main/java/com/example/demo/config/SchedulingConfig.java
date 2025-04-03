package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks in the application.
 * Used for periodic synchronization between MongoDB and Redis.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Configuration is handled via annotations
}