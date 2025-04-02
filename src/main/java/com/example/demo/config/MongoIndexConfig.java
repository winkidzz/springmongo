package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

import com.example.demo.model.Order;
import com.example.demo.model.ProductConfig;

@Configuration
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    public MongoIndexConfig(MongoTemplate mongoTemplate, MongoMappingContext mongoMappingContext) {
        this.mongoTemplate = mongoTemplate;
        this.mongoMappingContext = mongoMappingContext;
    }

    @Bean
    public boolean createIndexes() {
        // Create indexes for Order collection
        IndexOperations orderIndexOps = mongoTemplate.indexOps(Order.class);
        orderIndexOps.ensureIndex(new Index().on("orderDate", org.springframework.data.domain.Sort.Direction.ASC));
        orderIndexOps.ensureIndex(new Index().on("status", org.springframework.data.domain.Sort.Direction.ASC));
        orderIndexOps.ensureIndex(new Index().on("productId", org.springframework.data.domain.Sort.Direction.ASC));

        // Create indexes for ProductConfig collection
        IndexOperations productConfigIndexOps = mongoTemplate.indexOps(ProductConfig.class);
        productConfigIndexOps
                .ensureIndex(new Index().on("productId", org.springframework.data.domain.Sort.Direction.ASC));
        productConfigIndexOps
                .ensureIndex(new Index().on("enabled", org.springframework.data.domain.Sort.Direction.ASC));
        productConfigIndexOps
                .ensureIndex(new Index().on("startDate", org.springframework.data.domain.Sort.Direction.ASC));
        productConfigIndexOps
                .ensureIndex(new Index().on("endDate", org.springframework.data.domain.Sort.Direction.ASC));

        // Create compound indexes
        orderIndexOps.ensureIndex(new Index()
                .on("orderDate", org.springframework.data.domain.Sort.Direction.ASC)
                .on("status", org.springframework.data.domain.Sort.Direction.ASC)
                .on("productId", org.springframework.data.domain.Sort.Direction.ASC));

        productConfigIndexOps.ensureIndex(new Index()
                .on("productId", org.springframework.data.domain.Sort.Direction.ASC)
                .on("enabled", org.springframework.data.domain.Sort.Direction.ASC)
                .on("startDate", org.springframework.data.domain.Sort.Direction.ASC)
                .on("endDate", org.springframework.data.domain.Sort.Direction.ASC));

        return true;
    }
}