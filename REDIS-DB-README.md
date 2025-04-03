# Redis as a Primary Database for Product Configurations

This implementation uses Redis as a primary database for product configurations, providing ultra-fast access to product data with single-digit millisecond response times.

## Getting Started

### Prerequisites

- Redis server 6.0+ running on your environment
- Spring Boot 3.x
- Java 17+

### Configuration

Add the following to your `application.properties`:

```properties
# Redis Database Configuration
spring.data.redis.host=192.168.1.198
spring.data.redis.port=6379
spring.data.redis.timeout=60000

# Redis Persistence Settings (optional)
# Configure for production environments
# spring.data.redis.database=0 
# spring.data.redis.password=your_password
```

## Features

- Complete CRUD operations for product configurations
- Secondary indexing for fast lookups
- Auto-expiry for time-limited configurations
- Data synchronization from MongoDB/Elasticsearch
- Performance monitoring and metrics

## Usage

### API Endpoints

All endpoints are available at `/api/redis-db/`:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/configs` | Get all product configurations |
| GET | `/configs/{id}` | Get specific configuration by ID |
| GET | `/products/{productId}/configs` | Get configurations for a product |
| GET | `/active-configs` | Get all active configurations |
| GET | `/active-products` | Get distinct active product IDs |
| POST | `/configs` | Create a new configuration |
| PUT | `/configs/{id}` | Update a configuration |
| DELETE | `/configs/{id}` | Delete a configuration |
| POST | `/sync/mongodb` | Sync from MongoDB to Redis |
| POST | `/sync/elasticsearch` | Sync from Elasticsearch to Redis |
| DELETE | `/configs` | Clear all configurations |
| GET | `/info` | Get Redis database statistics |

### Example Usage

1. First, sync data from your existing database:

```bash
# Import from MongoDB
curl -X POST http://localhost:8081/api/redis-db/sync/mongodb

# Or import from Elasticsearch
curl -X POST http://localhost:8081/api/redis-db/sync/elasticsearch
```

2. Retrieve active products from Redis:

```bash
curl http://localhost:8081/api/redis-db/active-products
```

3. Add a new product configuration:

```bash
curl -X POST -H "Content-Type: application/json" http://localhost:8081/api/redis-db/configs \
  -d '{
    "productId": "NEW-PRODUCT-1",
    "enabled": true,
    "startDate": "2025-04-01T00:00:00",
    "endDate": "2025-12-31T23:59:59"
  }'
```

4. Check database info:

```bash
curl http://localhost:8081/api/redis-db/info
```

## Performance

Typical response times for Redis database operations:

- Read operations: 2-9 ms
- Write operations: 3-7 ms
- Bulk operations: 100-500 ms (depending on data volume)

This is significantly faster than MongoDB (650-900 ms) and Elasticsearch (77-380 ms) for the same operations.

## Data Synchronization

Redis can be used as:

1. **Complete replacement** for MongoDB/Elasticsearch
2. **Primary database** with periodic syncs to other databases
3. **Fast access layer** with real-time propagation of changes

The included synchronization APIs support all these patterns.

## Implementation Notes

### Model Design

```java
@RedisHash("product_config")
public class ProductConfigRedis implements Serializable {
    @Id
    private String id;
    
    @Indexed
    private String productId;
    
    @Indexed
    private boolean enabled;
    
    private LocalDateTime startDate;
    
    private LocalDateTime endDate;
    
    // ... getters and setters
}
```

### Repository Layer

```java
@Repository
public interface ProductConfigRedisRepository extends CrudRepository<ProductConfigRedis, String> {
    List<ProductConfigRedis> findByEnabledTrue();
    List<ProductConfigRedis> findByProductId(String productId);
    List<ProductConfigRedis> findByProductIdInAndEnabledTrue(List<String> productIds);
    void deleteByProductId(String productId);
}
```

## Best Practices

1. **Memory Management**: Monitor Redis memory usage and configure `maxmemory` and eviction policies
2. **Data Persistence**: Configure RDB and/or AOF for durability
3. **Indexing**: Use `@Indexed` annotation for fields that need fast lookups
4. **Transactions**: Use Redis transactions for operations that need to be atomic
5. **Monitoring**: Set up monitoring for Redis instances (memory, connections, operations)
6. **Security**: Configure authentication and network security for production
7. **Backup**: Schedule regular backups of Redis data

## Comparison with Caching

While Redis is often used as a cache, this implementation uses it as a full database with:

- Complete data ownership (not just a cache of another database)
- Data persistence configuration
- Full CRUD operations through repository abstraction
- Secondary indexes for efficient querying
- Custom query methods

## More Information

For more detailed performance analysis, see the `performance.md` document in the project root. 