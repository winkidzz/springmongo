package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

@Configuration
public class MongoIndexConfig implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Index for orders collection
        mongoTemplate.indexOps("orders").ensureIndex(new Index().on("status", Sort.Direction.ASC));
        mongoTemplate.indexOps("orders").ensureIndex(new Index().on("productId", Sort.Direction.ASC));

        // Compound index for product_configs collection
        mongoTemplate.indexOps("product_configs").ensureIndex(
                new Index()
                        .on("productId", Sort.Direction.ASC)
                        .on("enabled", Sort.Direction.ASC)
                        .on("startDate", Sort.Direction.ASC)
                        .on("endDate", Sort.Direction.ASC));
    }
}