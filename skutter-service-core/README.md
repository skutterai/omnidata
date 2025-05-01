# Skutter Service Core Library

A production-ready Spring Boot library providing common components and configurations for Skutter microservices.

## Overview

This library standardizes common concerns across microservices, including:

*   **Security:** JWT processing (Supabase), Role-based access, CORS, Security logging.
*   **REST API:** Standardized error handling, Rate limiting, Circuit breaking (Resilience4j), OpenAPI docs, Pagination models.
*   **Data Access:** JPA configuration, PostgreSQL optimizations, PostGIS support, **Centralized Flyway database schema management**.
*   **Observability:** Correlation IDs, Prometheus metrics, Actuator extensions, MDC logging.
*   **Utilities:** Environment configuration, Null-safety, Date/time handling, Encryption, ID generation, Type identification.
*   **CLI Support:** Provides a `CommandLineRunner` (like `FlywayCommandRunner`) for executing tasks independently (though Gradle is preferred for migrations).

## Features In Detail

### Security
*   **JWT:** Integrates with Supabase Auth for token validation. Extracts custom `skutter_role` claim. Configurable via `skutter.security.jwt.*` properties. Requires `JWT_SECRET` environment variable or `skutter.security.jwt.secret` property matching your Supabase secret.
*   **Roles:** Defines `SkutterRole` enum and provides `SupabaseUserDetails` for easy access to user ID and authorities. Enables method security with `@PreAuthorize`.
*   **CORS:** Configurable CORS policy.
*   **User ID Propagation:** Optionally propagates authenticated user ID to PostgreSQL using `SET LOCAL skutter.app.current_user_id = ?`. Enabled via `skutter.security.jwt.set-postgres-user-id=true`.

### REST API
*   **Error Handling:** `GlobalExceptionHandler` provides standardized JSON error responses (`ErrorResponse`).
*   **Rate Limiting:** Configurable request rate limiting via `skutter.api.rate-limit.*`.
*   **Resilience:** Integrates Resilience4j for Circuit Breaker patterns (`skutter.api.resilience.*`).
*   **Documentation:** Auto-generates OpenAPI v3 specification. Configurable via `skutter.api.documentation.*`.
*   **Pagination:** Standard `PagedResponse`, `CursorPageRequest`, `OffsetPageRequest`.

### Data Access
*   **JPA:** Configures JPA, connection pooling (HikariCP).
*   **Auditing:** Enables JPA auditing (`@CreatedDate`, `@LastModifiedDate`, etc.).
*   **PostGIS:** Includes `hibernate-spatial` and configures the PostgreSQL dialect for spatial types.
*   **Flyway Database Migrations:**
    *   **Owner of Shared Schema:** This module is the **single source of truth** for the shared database schema used by dependent services.
    *   **Migration Location:** SQL migration scripts reside exclusively in `src/main/resources/db/migration`. See the [README](./src/main/resources/db/migration/README.md) in that directory for details.
    *   **Naming Convention:** Files must follow the pattern `V<VERSION>__<DESCRIPTION>.sql`.
    *   **Applying Migrations (Gradle):** The primary way to apply or check migrations during development or CI is via Gradle tasks run against this module:
        ```bash
        # Apply pending migrations
        ./gradlew :skutter-service-core:flywayMigrate

        # Show migration status
        ./gradlew :skutter-service-core:flywayInfo

        # Validate applied migrations against available ones
        ./gradlew :skutter-service-core:flywayValidate

        # Clean the DB (Use with caution!)
        ./gradlew :skutter-service-core:flywayClean
        ```
        *   These tasks use database credentials configured in this module's `build.gradle` (loaded from `.env.integration-test` or environment variables like `SPRING_DATASOURCE_URL`/`USERNAME`/`PASSWORD`). Ensure the configured user has necessary permissions.
    *   **Runtime Validation:** Applications using this library typically have `spring.flyway.enabled=true` set in their `application.yml`. At startup, Spring Boot will use Flyway to validate the connected database schema against the migrations packaged within this library's JAR. It will **not** attempt to run migrations itself unless explicitly configured to do so (which is not the standard setup for dependent services).
    *   **Integration Tests:** Integration tests within *this* module (`src/integration-test`) may run Flyway migrations against a test database (e.g., using Testcontainers). Their configuration (e.g., `src/integration-test/resources/application-dev.yml` or `application-test.yml`) should manage Flyway settings (`create-schemas: true`, target schema/table) and database credentials for the test environment.

### Observability
*   **Correlation IDs:** `CorrelationIdFilter` generates/propagates IDs via headers (default: `X-Correlation-ID`) and MDC.
*   **Metrics:** Configures Micrometer for Prometheus endpoint (`/actuator/prometheus`). Adds common tags.
*   **Logging:** Provides utilities and an Actuator endpoint (`/actuator/loggers`) for managing log levels.

### Command-Line Interface (CLI)
*   **Purpose:** Allows running specific tasks from the command line using the application's configured context, without starting the full application (e.g., web server).
*   **Implementation:** Uses `CommandLineRunner` implementations (e.g., `FlywayCommandRunner`).
*   **Flyway via CLI:** The `FlywayCommandRunner` approach for running migrations via `java -jar your-application.jar flyway migrate` is **deprecated** in favor of the centralized `gradle :skutter-service-core:flywayMigrate` task for managing the shared schema. While the runner might still exist for backward compatibility or other non-migration tasks, relying on the Gradle task provides a clearer separation of concerns for schema evolution.

### Utilities
*   `DeterministicIdGenerator`: Creates stable short IDs.
*   `TypeIdentifier`: Detects common data types.
*   `DataSize`: Utilities for handling data sizes (KB, MB, etc.).

## Getting Started

### Prerequisites
*   Java 17+
*   Gradle 8.x

### Installation (for consuming services)

1.  **Publish the library:** Ensure this library is built (`./gradlew build`) and published to an accessible repository (e.g., GitHub Packages).
2.  **Add Dependency:** In the consuming service's `build.gradle`:
    ```gradle
    repositories {
        mavenCentral()
        // Add repository where skutter-service-core is published
        maven {
            name = 'GitHubPackages' // Or your repository name
            url = uri('https://maven.pkg.github.com/skutter/skutter-service-core') // Or your repo URL
            credentials { /* ... credentials ... */ }
        }
    }

    dependencies {
        implementation 'ai.skutter.common:skutter-service-core:0.1.0-SNAPSHOT' // Use the correct version
    }
    ```

### Configuration

The library uses Spring Boot auto-configuration. Customize behavior via `application.yml` or environment variables. Key prefixes:

*   `skutter.security.*`
*   `skutter.api.*`
*   `skutter.data.*`
*   `skutter.observability.*`
*   `skutter.actuator.*`
*   `spring.flyway.*` (Standard Spring Boot Flyway properties - note schema/table defaults are set)
*   `spring.datasource.*` (Standard Spring Boot Datasource properties)

**Essential Configuration:**

*   **Database:** `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` are needed for Flyway validation and JPA.
*   **JWT Security:** `skutter.security.jwt.secret` (or `JWT_SECRET` env var) and `skutter.security.jwt.issuer` (or `JWT_ISSUER` env var) matching your Supabase configuration if security is enabled.

## Building and Testing This Library

Refer to the [main project README](../README.md) for general build instructions. Remember to use the Gradle tasks specific to this module (e.g., `./gradlew :skutter-service-core:build`, `./gradlew :skutter-service-core:integrationTest`).

## Contributing

(Add specific contribution guidelines for this module if different from the main project).

## License

Apache License 2.0 