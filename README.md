# Spring Boot Product Service Demo

This is a demo Spring Boot application that provides a RESTful API for managing products and orders. The application is built with Spring Boot 3.2.3 and Java 21.

## Features

- RESTful API for product and order management
- MongoDB integration with advanced aggregation pipelines
- Spring Security configuration
- Actuator endpoints for monitoring
- Swagger UI for API documentation
- Performance testing capabilities

## Prerequisites

- Java 21
- Maven 3.x
- MongoDB (running on port 27018)

## Getting Started

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd springmongo
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

The application will start on port 8081.

## API Documentation

### Product Management

#### Create a Product
- **Endpoint**: `POST /api/products`
- **Description**: Creates a new product
- **Request Body**: Product object with required fields
- **Response**: Created product object

#### Get All Products
- **Endpoint**: `GET /api/products`
- **Description**: Retrieves all products
- **Response**: List of product objects

#### Get Product by ID
- **Endpoint**: `GET /api/products/{id}`
- **Description**: Retrieves a specific product by ID
- **Parameters**: 
  - `id`: Product ID
- **Response**: Product object

#### Update Product
- **Endpoint**: `PUT /api/products/{id}`
- **Description**: Updates an existing product
- **Parameters**:
  - `id`: Product ID
- **Request Body**: Updated product object
- **Response**: Updated product object

#### Delete Product
- **Endpoint**: `DELETE /api/products/{id}`
- **Description**: Deletes a product
- **Parameters**:
  - `id`: Product ID
- **Response**: Success/error message

### Order Management

#### Get All Orders
- **Endpoint**: `GET /api/orders`
- **Description**: Lists all orders in the order table
- **Implementation**: Uses standard `findAll()` method from MongoRepository
- **Response**: List of order objects

#### Get Active Orders
- **Endpoint**: `GET /api/orders/active`
- **Description**: Lists only active orders by joining with product_configs
- **Features**:
  - Filters for enabled configurations
  - Validates date ranges (within start and end dates)
  - Returns full order objects with associated product configurations
- **MongoDB Pipeline**:
  ```javascript
  [
    { $lookup: { 
      from: 'product_configs', 
      localField: 'productConfigId', 
      foreignField: '_id', 
      as: 'productConfig' 
    }},
    { $unwind: '$productConfig' },
    { $match: { 'productConfig.enabled': true } }
  ]
  ```
- **Response**: List of active order objects with their configurations

#### Get Active Product ID Summary
- **Endpoint**: `GET /api/orders/active/filter/productidsummary`
- **Description**: Returns a summary of distinct product IDs from active orders
- **Features**:
  - Includes count of orders and total amount for each product ID
  - Only includes products with active configurations
- **MongoDB Pipeline**:
  ```javascript
  [
    { $lookup: { 
      from: 'product_configs', 
      localField: 'productConfigId', 
      foreignField: '_id', 
      as: 'productConfig' 
    }},
    { $unwind: '$productConfig' },
    { $match: { 'productConfig.enabled': true } },
    { $group: { 
      _id: '$productId', 
      count: { $sum: 1 } 
    }}
  ]
  ```
- **Response**: List of product summaries with counts

### Performance Testing

#### Generate Test Data
- **Endpoint**: `POST /api/performance-test/generate`
- **Description**: Generates test data for performance testing
- **Parameters**:
  - `numOrders` (default: 1000): Number of orders to generate
  - `numConfigs` (default: 5): Number of configurations to generate
- **Response**: Generation status and counts

#### Get Statistics
- **Endpoint**: `GET /api/performance-test/stats`
- **Description**: Retrieves performance test statistics
- **Response**: Statistics displayed in logs

#### Clear Test Data
- **Endpoint**: `DELETE /api/performance-test/clear`
- **Description**: Clears all test data
- **Response**: Clear operation status

## Testing

Run the tests using:
```bash
mvn test
```

## Configuration

The application configuration can be found in `src/main/resources/application.properties`. Key configurations include:

- MongoDB connection settings
- Server port (default: 8081)
- Actuator endpoints
- Security settings

## MongoDB Aggregation Pipelines

The application uses several MongoDB aggregation pipelines for complex queries:

1. **Order Summaries Pipeline**:
   ```javascript
   [
     { $match: { 
       status: { $in: ['PENDING', 'PROCESSING', 'CANCELLED'] },
       price: { $gt: 10 },
       orderDate: { $gte: ?0 }
     }},
     { $lookup: { 
       from: 'product_configs',
       localField: 'productId',
       foreignField: 'productId',
       as: 'productConfig'
     }},
     { $unwind: '$productConfig' },
     { $match: {
       'productConfig.enabled': true,
       'productConfig.startDate': { $gte: ?0 },
       'productConfig.endDate': { $lte: ?1 }
     }},
     { $group: {
       _id: '$productName',
       totalOrders: { $sum: 1 },
       totalQuantity: { $sum: '$quantity' },
       totalPrice: { $sum: { $multiply: ['$price', '$quantity'] } },
       averagePrice: { $avg: '$price' },
       statusCounts: {
         $push: {
           status: '$status',
           count: 1
         }
       }
     }},
     { $project: {
       _id: 0,
       productName: '$_id',
       totalOrders: 1,
       totalQuantity: 1,
       totalPrice: 1,
       averagePrice: 1,
       statusBreakdown: {
         $map: {
           input: '$statusCounts',
           as: 'status',
           in: {
             status: '$$status.status',
             count: '$$status.count'
           }
         }
       }
     }}
   ]
   ```

2. **Order Summary Pipeline**:
   ```javascript
   [
     { $group: {
       _id: null,
       totalOrders: { $sum: 1 },
       totalAmount: { $sum: '$amount' },
       avgAmount: { $avg: '$amount' }
     }},
     { $project: {
       _id: 0,
       totalOrders: 1,
       totalAmount: 1,
       avgAmount: 1
     }}
   ]
   ```

## Management Script

The application includes a PowerShell management script (`spring-boot-management.ps1`) that provides functions for:

- Checking if Spring Boot is running
- Stopping Spring Boot processes
- Starting Spring Boot in a new shell
- Monitoring application health
- Troubleshooting startup issues

To use the management script:
```powershell
.\spring-boot-management.ps1
``` 