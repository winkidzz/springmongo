# Spring Boot MongoDB Aggregation Project

## Overview
This project demonstrates how to use Spring Boot with MongoDB to perform advanced aggregation operations. It focuses on building efficient aggregation pipelines for retrieving and analyzing data from MongoDB collections.

## Features
- REST API for managing orders
- MongoDB integration with Spring Data
- Advanced aggregation pipelines
- Custom repository implementations
- Optimized database queries with indexes
- Test data generation

## Tech Stack
- Spring Boot 3.2.3
- MongoDB
- Java 21
- Maven
- Spring Data MongoDB

## Key Components

### Data Models
- `Order`: Represents customer orders with fields like orderId, productId, status, etc.
- `ProductConfig`: Represents product configuration with fields like enabled, startDate, endDate, etc.

### API Endpoints
- `GET /api/orders/active-products`: Returns a list of distinct product IDs from completed orders where the product is currently active
- `GET /api/orders/by-date-range`: Returns orders within a specified date range
- `GET /api/orders/by-status`: Returns orders with a specified status
- `GET /api/orders/by-status-and-date`: Returns orders with a specified status within a date range
- `GET /api/orders/by-status-and-amount`: Returns orders with a specified status, within a date range, and above a minimum amount
- `POST /api/data/generate`: Generates test data for development and testing purposes

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

### Database Optimization
- Compound indexes to support the aggregation pipeline stages
- Single-field indexes for individual queries
- Efficient date range queries

## Performance Considerations
- The aggregation pipeline is optimized to filter data early in the process
- Indexes support the filtering criteria
- The `allowDiskUse` option enables processing of large datasets
- Performance metrics are logged for monitoring query execution times

## Setup and Configuration

### Prerequisites
- Java 21
- Maven
- MongoDB server

### Configuration
The MongoDB connection settings can be found in the `application.properties` file:

```properties
spring.data.mongodb.host=192.168.1.198
spring.data.mongodb.port=27018
spring.data.mongodb.database=orders
spring.data.mongodb.connect-timeout=5000
spring.data.mongodb.socket-timeout=10000
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
Get active products:
```bash
curl -X GET "http://localhost:8081/api/orders/active-products"
```

## Development Guidelines
- Follow the existing package structure
- Use Spring Data MongoDB repositories when possible
- Implement custom repository methods using the `MongoTemplate` for complex queries
- Always create appropriate indexes for query patterns
- Use the aggregation framework for complex data transformations and queries 