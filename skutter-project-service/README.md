# Skutter Project Service

This Spring Boot microservice manages project-related entities and demonstrates the usage of the `skutter-service-core` library.

## Overview

This service provides APIs for managing projects and their associated data. It relies heavily on the common functionalities provided by the `skutter-service-core` library.

## Database Schema

*   **Schema Source:** This service **does not** manage the shared database schema itself. The schema is defined and versioned using Flyway migrations located within the `skutter-service-core` module (`skutter-service-core/src/main/resources/db/migration`).
*   **Runtime Validation:** At startup, this service (via Spring Boot and the included `flyway-core` dependency from the core library) validates that the database it connects to matches the schema defined by the migrations in `skutter-service-core`. Ensure `spring.flyway.enabled=true` is set in `application.yml` (or the active profile's `.yml`).
*   **Applying Migrations:** Before running this service for the first time, or after any schema changes in `skutter-service-core`, you **must** apply the migrations using the Gradle task from the core module:
    ```bash
    # Run from the root project directory
    ../gradlew :skutter-service-core:flywayMigrate
    ```

## Building and Running

### Prerequisites

*   See the main project [README](../README.md).
*   Database must be running and migrated using the command above.

### Configuration

*   Uses configuration files (`application.yml`, `application-dev.yml`, etc.) located in `src/main/resources`.
*   Requires database connection details (`spring.datasource.*`) and potentially JWT secrets (`skutter.security.jwt.*` or environment variables) depending on the active profile and enabled features.
*   The `.env` file in the project root is typically used to provide credentials when running with the `dev` profile via `bootRun`.

### Gradle Commands (Run from root project directory)

*   **Build:**
    ```bash
    ../gradlew :skutter-project-service:build
    ```
*   **Run (Requires DB Migrations applied first):**
    ```bash
    # Uses settings from application-dev.yml by default (via SPRING_PROFILES_ACTIVE=dev in .env)
    ../gradlew :skutter-project-service:bootRun
    ```
*   **Create Docker Image (Jib):**
    ```bash
    ../gradlew :skutter-project-service:jibDockerBuild
    ```

## API Documentation

Once running, API documentation (Swagger UI) is typically available at `/swagger-ui.html` (adjust context path if necessary based on `server.servlet.context-path` in `application.yml`).

## Tests

*   **Unit Tests:**
    ```bash
    ../gradlew :skutter-project-service:test
    ```
*   **Integration Tests (if any):**
    ```bash
    ../gradlew :skutter-project-service:integrationTest
    ```
    *(Integration tests specific to this service might require their own setup, potentially involving mocking interactions with the database layer since the schema is managed externally).*
