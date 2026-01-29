# Ordering Web App – Food Ordering System (Backend)

A personal project focused on building a production-like food ordering backend with Spring Boot:
product catalog, cart, checkout/payment flow, order management, and admin features.
Includes integration tests using Testcontainers (PostgreSQL).

## Tech Stack
- Java 21, Spring Boot
- Spring Data JPA, Spring Security (JWT)
- PostgreSQL
- Testing: JUnit 5, Mockito, Testcontainers
- API Docs: Swagger / OpenAPI

## Key Features
- REST APIs for products, orders, payments, users, and admin management
- Checkout and order status workflow (e.g., NEW → PREPARING → DONE)
- Best-seller logic (sales-based or admin-tagged)
- JWT-based authentication and role-based authorization (USER/ADMIN)
- Database versioning with Flyway
- Integration tests with Testcontainers to verify real DB interactions
- Swagger UI for API exploration

## Architecture Overview
Layered architecture with clear separation of concerns:
- Controller (API layer)
- Service (business logic)
- Repository (data access)
- Domain (entities/value objects)
- DTOs
- Global exception handling + validation

## Project Structure

## Configuration
Use environment variables and `application-local.yml`.

Example env variables:
- `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/restaurant`
- `SPRING_DATASOURCE_USERNAME=...`
- `SPRING_DATASOURCE_PASSWORD=...`
- `JWT_SECRET=...`
- `STRIPE_API_KEY=...`
- `STRIPE_WEBHOOK_SECRET=...`
- `AWS_REGION=...`


