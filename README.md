# Spring Boot Product Service Demo

This is a demo Spring Boot application that provides a RESTful API for managing products. The application is built with Spring Boot 3.2.3 and Java 21.

## Features

- RESTful API for product management (CRUD operations)
- MongoDB integration
- Spring Security configuration
- Actuator endpoints for monitoring
- Swagger UI for API documentation

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

## API Endpoints

- Create a product: `POST /api/products`
- Get all products: `GET /api/products`
- Get a product: `GET /api/products/{id}`
- Update a product: `PUT /api/products/{id}`
- Delete a product: `DELETE /api/products/{id}`

## Testing

Run the tests using:
```bash
mvn test
```

## Configuration

The application configuration can be found in `src/main/resources/application.properties`. 