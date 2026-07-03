# Product Service - Java Spring Boot Microservice

A lightweight Spring Boot microservice designed for microfrontend integration. Provides a RESTful API for product management with full CORS support.

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL database (see [Database Setup](#database-setup))

## Database Setup

This service uses **PostgreSQL** for persistent storage via Spring Data JPA. In production the database is hosted on [Neon](https://neon.tech) (free tier, no expiry).

### Neon (Production / Render)

1. Create a free account at [neon.tech](https://neon.tech).
2. Create a new project (e.g. `product-service`).
3. Copy the **JDBC connection string** from the Neon dashboard.  
   It looks like: `jdbc:postgresql://ep-xxx.region.aws.neon.tech/neondb?user=...&password=...&sslmode=require`
4. Set the `DATABASE_URL` environment variable in the Render dashboard for this service.

### Local Development

Option A — **Local PostgreSQL**:
```bash
createdb product_db
DATABASE_URL=jdbc:postgresql://localhost:5432/product_db mvn spring-boot:run
```

Option B — **Use your Neon database directly**:
```bash
DATABASE_URL="jdbc:postgresql://ep-xxx.region.aws.neon.tech/neondb?user=...&password=...&sslmode=require" mvn spring-boot:run
```

### Tests

Tests use an **H2 in-memory database** automatically — no database setup required.

```bash
mvn test
```

## Build & Run

```bash
mvn clean package
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

## API Endpoints

| Method | Endpoint             | Description         |
|--------|----------------------|---------------------|
| GET    | /api/products        | List all products; optional `?search=` term filters by name, description or category (case-insensitive) |
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

### Searching / Filtering Products

Pass an optional `search` query parameter to `GET /api/products` to limit the
results to products whose **name**, **description** or **category** contains the
term (case-insensitive). Omitting or leaving the parameter blank returns all
products.

```javascript
// Only products matching "keyboard"
const response = await fetch('http://localhost:8080/api/products?search=keyboard');
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
