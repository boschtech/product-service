# Product Service - Java Spring Boot Microservice

A lightweight Spring Boot microservice designed for microfrontend integration. Provides a RESTful API for product management with full CORS support.

## Prerequisites

- Java 17+
- Maven 3.8+

## Build & Run

```bash
mvn clean package
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

## API Endpoints

| Method | Endpoint             | Description         |
|--------|----------------------|---------------------|
| GET    | /api/products        | List all products   |
| GET    | /api/products/{id}   | Get product by ID   |
| POST   | /api/products        | Create a product    |
| PUT    | /api/products/{id}   | Update a product    |
| DELETE | /api/products/{id}   | Delete a product    |
| GET    | /actuator/health     | Health check        |

## Microfrontend Integration

### CORS Configuration

The service allows cross-origin requests from these origins by default (configurable in `application.yml`):

- `http://localhost:3000` (React)
- `http://localhost:4200` (Angular)
- `http://localhost:5173` (Vite/Vue)

### Example Fetch (from microfrontend)

```javascript
const response = await fetch('http://localhost:8080/api/products');
const products = await response.json();
```

### Example POST

```javascript
await fetch('http://localhost:8080/api/products', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    name: 'New Product',
    description: 'A great product',
    price: 29.99,
    category: 'General'
  })
});
```
