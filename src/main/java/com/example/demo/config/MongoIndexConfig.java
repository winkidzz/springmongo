package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class MongoIndexConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MongoIndexConfig.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        createIndexes();
    }

    private void createIndexes() {
        // Indexes for orders collection
        IndexOperations orderIndexOps = mongoTemplate.indexOps("orders");

        // Compound index for orders query
        orderIndexOps.ensureIndex(new Index()
                .on("status", org.springframework.data.domain.Sort.Direction.ASC)
                .on("price", org.springframework.data.domain.Sort.Direction.ASC)
                .on("orderDate", org.springframework.data.domain.Sort.Direction.DESC)
                .on("productId", org.springframework.data.domain.Sort.Direction.ASC));

        // Indexes for product_configs collection
        IndexOperations configIndexOps = mongoTemplate.indexOps("product_configs");

        // Compound index for product configs query
        configIndexOps.ensureIndex(new Index()
                .on("enabled", org.springframework.data.domain.Sort.Direction.ASC)
                .on("startDate", org.springframework.data.domain.Sort.Direction.ASC)
                .on("endDate", org.springframework.data.domain.Sort.Direction.ASC)
                .on("productId", org.springframework.data.domain.Sort.Direction.ASC));

        logger.info("MongoDB indexes created successfully");
    }
}