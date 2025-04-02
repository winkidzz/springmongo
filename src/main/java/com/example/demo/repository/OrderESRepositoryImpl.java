package com.example.demo.repository;

import com.example.demo.model.OrderES;
import com.example.demo.model.ProductConfigES;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class OrderESRepositoryImpl implements OrderESRepositoryCustom {

    private static final Logger logger = LoggerFactory.getLogger(OrderESRepositoryImpl.class);

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private ProductConfigESRepository productConfigESRepository;

    @Override
    public List<String> findDistinctActiveProductsES() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Elasticsearch two-step query for distinct active products");

        // Step 1: Get all completed orders
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.termQuery("status", "COMPLETED"))
                .build();

        SearchHits<OrderES> searchHits = elasticsearchOperations.search(searchQuery, OrderES.class);

        // Extract distinct product IDs
        Set<String> productIds = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(OrderES::getProductId)
                .collect(Collectors.toSet());

        long step1EndTime = System.currentTimeMillis();
        logger.info("Step 1: Found {} distinct product IDs in {} ms",
                productIds.size(), (step1EndTime - startTime));

        if (productIds.isEmpty()) {
            return List.of();
        }

        // Step 2: Get active product configurations for these IDs
        LocalDateTime now = LocalDateTime.now();
        List<ProductConfigES> activeConfigs = productConfigESRepository
                .findByProductIdInAndEnabledTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        new ArrayList<>(productIds), now, now);

        List<String> results = activeConfigs.stream()
                .map(ProductConfigES::getProductId)
                .distinct()
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        logger.info("Step 2: Filter for active configs completed in {} ms",
                (endTime - step1EndTime));
        logger.info("Total execution time: {} ms, found {} active products",
                (endTime - startTime), results.size());

        return results;
    }

    @Override
    public List<String> findDistinctActiveProductsESNative() {
        long startTime = System.currentTimeMillis();
        logger.info("Starting Elasticsearch native aggregation query for distinct active products");

        // Step 1: Use Elasticsearch aggregation to get distinct productIds from
        // completed orders
        TermsAggregationBuilder aggregation = AggregationBuilders
                .terms("distinct_product_ids")
                .field("productId")
                .size(10000); // Set a high size to ensure we get all distinct values

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.termQuery("status", "COMPLETED"))
                .addAggregation(aggregation)
                .build();

        SearchHits<OrderES> searchHits = elasticsearchOperations.search(searchQuery, OrderES.class);

        // Extract the distinct product IDs from the aggregation result
        @SuppressWarnings("unchecked")
        List<String> distinctProductIds = (List<String>) searchHits.getAggregations()
                .get("distinct_product_ids")
                .getMetadata()
                .get("buckets")
                .stream()
                .map(bucket -> ((org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket) bucket)
                        .getKeyAsString())
                .collect(Collectors.toList());

        long step1EndTime = System.currentTimeMillis();
        logger.info("Step 1: Native ES aggregation completed in {} ms, found {} distinct products",
                (step1EndTime - startTime), distinctProductIds.size());

        if (distinctProductIds.isEmpty()) {
            return List.of();
        }

        // Step 2: Get active product configurations for these IDs
        LocalDateTime now = LocalDateTime.now();
        List<ProductConfigES> activeConfigs = productConfigESRepository
                .findByProductIdInAndEnabledTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        distinctProductIds, now, now);

        List<String> results = activeConfigs.stream()
                .map(ProductConfigES::getProductId)
                .distinct()
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        logger.info("Step 2: Filter for active configs completed in {} ms",
                (endTime - step1EndTime));
        logger.info("Total execution time: {} ms, found {} active products",
                (endTime - startTime), results.size());

        return results;
    }
}