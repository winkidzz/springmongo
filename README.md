# Spring Boot MongoDB, Elasticsearch, and Redis Integration

## Overview
This project demonstrates how to use Spring Boot with MongoDB, Elasticsearch, and Redis to build high-performance, scalable data access patterns. It focuses on optimizing data retrieval through various strategies including aggregation pipelines, full-text search, caching, and dual-write architectures.

## Features
- REST API for managing orders and product configurations
- MongoDB integration with Spring Data
- Elasticsearch for optimized text search
- Redis for caching and as a primary database
- Redis-MongoDB dual-write architecture for high performance and data durability
- Advanced aggregation pipelines
- Performance testing and visualization dashboard
- Custom repository implementations
- Optimized database queries with indexes
- Test data generation
- Database statistics monitoring

## Tech Stack
- Spring Boot 3.2.3
- MongoDB
- Elasticsearch
- Redis
- Java 21
- Maven
- Spring Data MongoDB/Elasticsearch/Redis

## Architecture

The project implements several data access patterns:

1. **MongoDB Direct**: Traditional MongoDB queries and aggregations
2. **Elasticsearch Optimized**: Full-text search optimized for performance
3. **Redis Caching**: Redis as a cache in front of MongoDB or Elasticsearch
4. **Redis as Primary Database**: Using Redis as the primary store for product configurations
5. **Redis-MongoDB Dual-Write**: High-performance architecture with Redis for speed and MongoDB for durability

## Key Components

### Data Models
- `Order`: Represents customer orders with fields like orderId, productId, status, etc.
- `ProductConfig`: Represents product configuration with fields like enabled, startDate, endDate, etc.
- `ProductConfigRedis`: Redis-specific model for product configurations

### API Endpoints

#### MongoDB Endpoints
- `GET /api/orders/active-products`: Returns a list of distinct product IDs from completed orders where the product is currently active
- `GET /api/orders/by-date-range`: Returns orders within a specified date range
- `GET /api/orders/by-status`: Returns orders with a specified status
- `GET /api/orders/by-status-and-date`: Returns orders with a specified status within a date range
- `GET /api/orders/by-status-and-amount`: Returns orders with a specified status, within a date range, and above a minimum amount

#### Elasticsearch Endpoints
- `GET /api/elasticsearch/products/active`: Returns a list of distinct active product IDs using Elasticsearch
- `GET /api/elasticsearch/products/active/optimized`: Returns active product IDs using highly optimized Elasticsearch queries

#### Redis Cached Endpoints
- `GET /api/redis-cached/products/active`: Returns active product IDs with Redis caching

#### Redis as Primary Database
- `GET /api/redis-db/products`: Get all product configurations from Redis
- `GET /api/redis-db/products/{id}`: Get a product configuration by ID
- `POST /api/redis-db/products`: Create a new product configuration
- `PUT /api/redis-db/products/{id}`: Update a product configuration
- `DELETE /api/redis-db/products/{id}`: Delete a product configuration

#### Dual-Write Architecture Endpoints
- `GET /api/products/active`: Get all active product IDs (from Redis with MongoDB fallback)
- `GET /api/products/config/{id}`: Get a specific product configuration
- `POST /api/products/config`: Create a new product configuration in both databases
- `PUT /api/products/config/{id}`: Update a product configuration in both databases
- `DELETE /api/products/config/{id}`: Delete a product configuration from both databases
- `POST /api/products/sync`: Manually sync MongoDB data to Redis
- `GET /api/products/consistency-check`: Check consistency between MongoDB and Redis

#### Order Management for Active Products
- `GET /api/orders/active`: Get all orders for active products
- `GET /api/orders/active-products`: Get all active products that have orders
- `GET /api/orders/product/{productId}`: Get orders for a specific active product
- `GET /api/orders/product/{productId}/stats`: Get order statistics for an active product

#### Utility Endpoints
- `POST /api/data/generate`: Generates test data for development and testing purposes
- `GET /api/db-stats/collections`: Returns statistics about MongoDB collections and database
- `GET /api/performance/test`: Run performance tests across all database implementations
- `POST /api/performance/warmup`: Warm up systems before running performance tests
- `GET /dashboard`: Access the performance testing dashboard

### Performance Dashboard

The application includes a visual dashboard for comparing performance across different database implementations:

```
http://localhost:8081/dashboard
```

The dashboard allows you to:
- Configure test parameters (iterations and concurrent users)
- Warm up systems before testing
- Run performance tests against all implementations
- View charts comparing performance
- See detailed metrics in tabular format

### MongoDB Aggregation Pipeline
The core feature of this application is the optimized aggregation pipeline for retrieving active products:

```java
// Match stage for completed orders
MatchOperation matchOrders = Aggregation.match(
        Criteria.where("status").is("COMPLETED"));

// Lookup stage for product configurations
LookupOperation lookupProductConfigs = Aggregation.lookup()
        .from("product_configs")
        .localField("productId")
        .foreignField("productId")
        .as("productConfig");

// Unwind the product config array
UnwindOperation unwindProductConfig = Aggregation.unwind("productConfig");

// Match stage for active product configurations
MatchOperation matchActiveConfigs = Aggregation.match(
        Criteria.where("productConfig.enabled").is(true)
                .and("productConfig.startDate").lte(now)
                .and("productConfig.endDate").gte(now));

// Group by productId to get distinct values
GroupOperation groupByProductId = Aggregation.group("productId");

// Project stage to format the output
ProjectionOperation project = Aggregation.project()
        .and("_id").as("productId");
```

### Dual-Write Architecture

The project implements a resilient dual-write architecture that provides:
- Ultra-fast access times using Redis
- Data durability with MongoDB as source of truth
- Automatic fallback to MongoDB if Redis is unavailable
- Scheduled synchronization to maintain consistency
- Manual sync and consistency checking capabilities

See [DUAL-WRITE-README.md](DUAL-WRITE-README.md) for detailed information.

### Redis as Primary Database

The project also demonstrates using Redis as a primary database for product configurations:
- Built-in indexing with annotation-based configuration
- Redis repositories for CRUD operations
- Spring Data Redis for simplified integration

See [REDIS-DB-README.md](REDIS-DB-README.md) for detailed information.

## Performance Considerations
- MongoDB aggregation pipeline optimized to filter data early in the process
- Elasticsearch using terms aggregations and minimal source filtering
- Redis providing sub-10ms response times for active product queries
- Dual-write architecture balancing performance and durability
- Caching strategies to minimize database calls

## Setup and Configuration

### Prerequisites
- Java 21
- Maven
- MongoDB server
- Elasticsearch server
- Redis server

### Configuration
Database connection settings can be found in the `application.properties` file:

```properties
# MongoDB Configuration
spring.data.mongodb.host=192.168.1.198
spring.data.mongodb.port=27018
spring.data.mongodb.database=orders
spring.data.mongodb.connect-timeout=5000
spring.data.mongodb.socket-timeout=10000

# Elasticsearch Configuration
spring.elasticsearch.rest.uris=http://192.168.1.198:9200

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000

# Synchronization Interval (in milliseconds)
redis.sync.interval=3600000
```

### Running the Application
```bash
mvn spring-boot:run
```

## Test Data Generation
The application includes a data generator that creates test orders and product configurations:

```bash
curl -X POST "http://localhost:8081/api/data/generate"
```

## Example Queries

Get active products from Redis-MongoDB dual-write:
```bash
curl -X GET "http://localhost:8081/api/products/active"
```

Synchronize MongoDB data to Redis:
```bash
curl -X POST "http://localhost:8081/api/products/sync"
```

Get orders for active products:
```bash
curl -X GET "http://localhost:8081/api/orders/active"
```

Run performance tests:
```bash
curl -X GET "http://localhost:8081/api/performance/test?iterations=5&concurrent=10"
```

## Development Guidelines
- Follow the existing package structure
- Use Spring Data repositories when possible
- Implement custom repository methods for complex queries
- Always create appropriate indexes for query patterns
- Use the aggregation framework for complex data transformations
- Consider data consistency requirements when using multi-database architectures
- Run performance tests to validate optimizations 