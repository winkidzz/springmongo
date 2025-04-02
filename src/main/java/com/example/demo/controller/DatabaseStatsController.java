package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/db-stats")
public class DatabaseStatsController {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public DatabaseStatsController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/collections")
    public ResponseEntity<Map<String, Object>> getCollectionStats() {
        Map<String, Object> stats = new HashMap<>();

        // Get collection names and their document counts
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            long count = mongoTemplate.getCollection(collectionName).countDocuments();
            stats.put(collectionName + "_count", count);

            try {
                // Get basic stats for each collection
                Document collStats = mongoTemplate.getDb()
                        .runCommand(new Document("collStats", collectionName)
                                .append("scale", 1024)); // Scale to KB

                // Extract the most useful stats - safely handling number types
                stats.put(collectionName + "_size_kb", getNumberValue(collStats, "size"));
                stats.put(collectionName + "_avg_doc_size_bytes", getNumberValue(collStats, "avgObjSize"));
                stats.put(collectionName + "_storage_size_kb", getNumberValue(collStats, "storageSize"));
                stats.put(collectionName + "_indexed", collStats.getBoolean("capped", false));
                stats.put(collectionName + "_num_indexes", collStats.get("nindexes"));
                stats.put(collectionName + "_total_index_size_kb", getNumberValue(collStats, "totalIndexSize"));
            } catch (Exception e) {
                stats.put(collectionName + "_error", "Error fetching stats: " + e.getMessage());
            }
        }

        try {
            // Get database stats
            Document dbStats = mongoTemplate.getDb()
                    .runCommand(new Document("dbStats", 1)
                            .append("scale", 1024)); // Scale to KB

            stats.put("database_name", dbStats.getString("db"));
            stats.put("database_size_kb", getNumberValue(dbStats, "dataSize"));
            stats.put("database_storage_size_kb", getNumberValue(dbStats, "storageSize"));
            stats.put("database_num_collections", getNumberValue(dbStats, "collections"));
            stats.put("database_num_views", getNumberValue(dbStats, "views"));
            stats.put("database_num_indexes", getNumberValue(dbStats, "indexes"));
        } catch (Exception e) {
            stats.put("database_error", "Error fetching database stats: " + e.getMessage());
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * Helper method to safely get a number value from a MongoDB document
     * regardless of whether it's stored as Integer, Long, or Double
     */
    private double getNumberValue(Document doc, String key) {
        Object value = doc.get(key);
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }

        return 0.0; // Default value if type is not recognized
    }
}