# Skutter Service Core Library

A production-ready Spring Boot library providing common components and configurations for Skutter microservices.

## Overview

This library standardizes common concerns across microservices, including:

*   **Security:** JWT processing (Supabase), Role-based access, CORS, Security logging.
*   **REST API:** Standardized error handling, Rate limiting, Circuit breaking (Resilience4j), OpenAPI docs, Pagination models.
*   **Data Access:** JPA configuration, PostgreSQL optimizations, PostGIS support, **Flyway database migrations**.
*   **Observability:** Correlation IDs, Prometheus metrics, Actuator extensions, MDC logging.
*   **Utilities:** Environment configuration, Null-safety, Date/time handling, Encryption, ID generation, Type identification.
*   **CLI Support:** Provides a `CommandLineRunner` (`FlywayCommandRunner`) for executing tasks like database migrations independently.

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
    *   Integrates Flyway for schema management. Enabled by default (`spring.flyway.enabled=true`).
    *   Looks for migration scripts in `classpath:db/migration`.
    *   Requires scripts named `V<VERSION>__<DESCRIPTION>.sql` (e.g., `V1_0__Create_projects_table.sql`).
    *   Applications using this library can run migrations automatically on startup or manually via the CLI (see below).
    *   Provides Gradle tasks (`flywayMigrate`, `flywayInfo`, etc.) configured to use environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` or fallbacks like `SKUTTER_DB_URL`).

### Observability
*   **Correlation IDs:** `CorrelationIdFilter` generates/propagates IDs via headers (default: `X-Correlation-ID`) and MDC.
*   **Metrics:** Configures Micrometer for Prometheus endpoint (`/actuator/prometheus`). Adds common tags.
*   **Logging:** Provides utilities and an Actuator endpoint (`/actuator/loggers`) for managing log levels.

### Command-Line Interface (CLI)
*   **Purpose:** Allows running specific tasks from the command line using the application's configured context, without starting the full application (e.g., web server).
*   **Implementation:** Uses `FlywayCommandRunner` (an implementation of `CommandLineRunner` and `ExitCodeGenerator`).

#### Flyway Database Management Commands

The CLI provides direct access to Flyway database migration commands. This is useful for applying migrations manually or inspecting the database state without running the full service.

##### Prerequisites

1.  **Build the Application:** Ensure the service is built into an executable JAR file:
    ```bash
    ./gradlew bootJar
    ```
    This creates a JAR file in the `build/libs/` directory.

2.  **Configuration:** The CLI uses the same configuration as the main application. Ensure database connection details are available via:
    *   Environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`)
    *   Application properties files
    *   System properties when running the command

##### Supported Commands

Run commands using the format: `java -jar your-application.jar flyway <command>`

*   `flyway info`
    Displays the status of all migrations (current version, applied migrations, pending migrations).

*   `flyway migrate`
    Applies any pending migrations to the database schema.

*   `flyway validate`
    Validates the applied migrations against the ones available on the classpath.

*   `flyway clean`
    **WARNING:** Drops all objects within the configured database schemas. Use with extreme caution, typically only in development/testing.

##### Examples

```bash
# Apply migrations using dev profile
java -Dspring.profiles.active=dev -jar build/libs/your-application.jar flyway migrate

# Check migration status
java -jar build/libs/your-application.jar flyway info

# Validate migrations
java -jar build/libs/your-application.jar flyway validate
```

##### Exit Codes
*   `0`: Success
*   `1`: Command failed during execution
*   `2`: Invalid/unsupported command
*   `3`: Unhandled internal error

##### Technical Details
*   The `FlywayCommandRunner` is active only if a `Flyway` bean is present (`@ConditionalOnBean(Flyway.class)`).
*   Disabled in the "test" profile by default.
*   Can be extended with more commands by modifying `SUPPORTED_COMMANDS` and the command handling logic.

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
*   `spring.flyway.*` (Standard Spring Boot Flyway properties)
*   `spring.datasource.*` (Standard Spring Boot Datasource properties)

**Essential Configuration:**

*   **Database:** `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` are needed for Flyway and JPA.
*   **JWT Security:** `skutter.security.jwt.secret` (or `JWT_SECRET` env var) and `skutter.security.jwt.issuer` (or `JWT_ISSUER` env var) matching your Supabase configuration.

## Building and Testing This Library

Refer to the [main project README](../README.md) for instructions on building the project and running unit/integration tests.

## Contributing

(Add specific contribution guidelines for this module if different from the main project).

## License

Apache License 2.0 