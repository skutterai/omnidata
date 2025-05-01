# Skutter Omnidata Project

This repository contains the core components for Skutter's data services.

## Structure

This is a multi-module Gradle project:

*   **`skutter-service-core`**: A production-ready Spring Boot library providing shared components (security, data access, observability, API handling, etc.). **This module now manages the shared database schema.**
*   **`skutter-project-service`**: A Spring Boot microservice demonstrating usage of `skutter-service-core` and managing project-specific logic. It relies on `skutter-service-core` for its database schema.
*   **(Future services)**: Additional services depending on `skutter-service-core` can be added.

## Database Schema Management (Flyway)

*   **Single Source of Truth:** The shared PostgreSQL database schema is defined and versioned using Flyway migrations located exclusively in:
    `skutter-service-core/src/main/resources/db/migration`
*   **Applying Migrations:** Migrations **must** be applied using the Gradle task from the `skutter-service-core` module *before* running dependent services if the schema has changed:
    ```bash
    # Ensure DB connection details are available (e.g., via .env.integration-test or environment variables)
    ./gradlew :skutter-service-core:flywayMigrate
    ```
*   **Checking Status:** To see the current migration status:
    ```bash
    ./gradlew :skutter-service-core:flywayInfo
    ```
*   **Runtime Validation:** Services using `skutter-service-core` (like `skutter-project-service`) will validate the database schema against the migrations found in the core library's JAR at startup (if `spring.flyway.enabled=true`).

## Getting Started

### Prerequisites

*   Java 17 (Adoptium Temurin recommended)
*   Gradle (uses wrapper, `./gradlew`)
*   Docker and Docker Compose (for running dependencies like PostgreSQL)
*   Access to a Supabase project (for JWT validation if security is enabled)

### Environment Setup

Key services and configurations rely on environment variables.

1.  **Core Development Configuration (`.env`):**
    *   Used primarily by `skutter-project-service` when running locally via `bootRun` or IDE.
    *   Create `.env` in the project root (`/Users/mattduggan/Documents/skutter/omnidata`) from `.env-template`.
    *   Configure `SPRING_DATASOURCE_URL`, `USERNAME`, `PASSWORD` for your development DB.
    *   Configure Supabase/JWT variables (`JWT_SECRET`, `JWT_ISSUER`) if security is enabled.

2.  **Gradle Task / Integration Test Configuration (`.env.integration-test`):**
    *   Used by Gradle tasks in `skutter-service-core` (like `flywayMigrate`, `integrationTest`) and potentially by integration tests directly.
    *   Create `.env.integration-test` in the project root from `skutter-service-core/src/integration-test/resources/env-integration-test.template` (if it exists) or `.env-template`.
    *   **Crucially, ensure the database user configured here has permissions to CREATE SCHEMA and modify the target database**, as required by Flyway and integration tests.
    *   Configure Supabase/JWT variables if integration tests require them.

### Common Gradle Commands

Navigate to the project root directory (`/Users/mattduggan/Documents/skutter/omnidata`).

*   **Full Build (Compile, Tests, Checks, Assemble):**
    ```bash
    ./gradlew build
    ```
    *Use `-x test -x integrationTest` to skip tests.*

*   **Clean Build:**
    ```bash
    ./gradlew clean build
    ```

*   **Run Database Migrations (Required before first run/after schema changes):**
    ```bash
    ./gradlew :skutter-service-core:flywayMigrate
    ```

*   **Check Database Migration Status:**
    ```bash
    ./gradlew :skutter-service-core:flywayInfo
    ```

*   **Run Project Service (Requires DB to be migrated first):**
    ```bash
    ./gradlew :skutter-project-service:bootRun
    ```

*   **Run Core Module Integration Tests (Requires Docker, `.env.integration-test`):**
    ```bash
    ./gradlew :skutter-service-core:integrationTest
    ```

### Configuration Files

Spring Boot configuration is loaded from:
*   `application.yml` (defaults)
*   `application-{profile}.yml` (profile-specific overrides, e.g., `application-dev.yml`)
*   Environment Variables (highest priority)
*   `.env` files (loaded by build scripts for specific tasks/contexts)

## Modules

*   **[`skutter-service-core`](./skutter-service-core/README.md):** Core library details.
*   **[`skutter-project-service`](./skutter-project-service/README.md):** Project service details.

## Contributing

(Add contribution guidelines here).

## License

(Specify project license).
