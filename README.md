# Skutter Omnidata Project

This repository contains the core components for Skutter's data services, primarily featuring the `skutter-service-core` library.

## Overview

The `omnidata` project provides foundational building blocks for Skutter microservices.

*   **`skutter-service-core`**: A production-ready Spring Boot library project serving as a shared dependency. It provides common configurations, utilities, and base classes for security, data access, observability, and API handling to accelerate development and ensure consistency.
*   **(Other modules, if any, would be listed here)**

## `skutter-service-core` Library

(This section summarizes the core library - keep it relatively brief, linking to the module's README for details)

The `skutter-service-core` library includes reusable components for:

*   **Security:** JWT validation (Supabase Auth), Role-based access control, CORS, HTTPS enforcement.
*   **Observability:** Correlation ID handling, Prometheus metrics, logging utilities, Actuator endpoints.
*   **Data Access:** JPA Auditing, PostgreSQL utilities, **Flyway database migrations**, PostGIS support.
*   **API Handling:** Global exception handling, Rate limiting, Resilience patterns (Circuit Breaker, Retry), Standardized pagination and error responses.
*   **Utilities:** Deterministic ID Generation, Type Identification, Data Size utilities.
*   **CLI Support:** Allows running tasks like database migrations independently via the command line.

**For detailed information, usage, and configuration options, see the [skutter-service-core README](./skutter-service-core/README.md).**

## Getting Started

### Prerequisites

*   Java 17 (Adoptium Temurin recommended)
    *   Gradle (uses wrapper, `./gradlew`)
*   Docker and Docker Compose (for running dependencies like PostgreSQL)
*   Access to a Supabase project (for integration tests requiring JWT validation)

### Environment Setup

Key services and configurations rely on environment variables.

1.  **Core Database Configuration:**
    *   Create a `.env` file in the project root (`/Users/mattduggan/Documents/skutter/omnidata`).
    *   Use the `.env-template` file as a guide:
        ```bash
        cp .env-template .env
        ```
    *   Edit `.env` and set at least:
        *   `SPRING_DATASOURCE_URL`: The full JDBC URL (e.g., `jdbc:postgresql://localhost:5432/skutter`)
        *   `SPRING_DATASOURCE_USERNAME`: Your PostgreSQL username (e.g., `postgres`)
        *   `SPRING_DATASOURCE_PASSWORD`: Your PostgreSQL password
        *   Optionally `SKUTTER_DB`, `PG_PORT` if other tools use them.

2.  **Integration Test Configuration:**
    *   Integration tests require additional credentials, primarily for Supabase.
    *   Create a `.env.integration-test` file in the project root.
    *   Use the template as a guide:
        ```bash
        cp skutter-service-core/src/integration-test/resources/env-integration-test.template .env.integration-test
        ```
    *   Edit `.env.integration-test` with your actual Supabase URL, API Key, JWT Secret, and test user credentials. **The `JWT_SECRET` is crucial for token validation tests.**

### Building the Project

Navigate to the project root directory (`/Users/mattduggan/Documents/skutter/omnidata`).

*   **Full Build (Compile, Unit Tests, Checks, Assemble JARs):**
    ```bash
    ./gradlew build
    ```
    *Use `-x test -x integrationTest -x check` to skip tests and checks for a faster build.*

*   **Clean Build:**
    ```bash
    ./gradlew clean build
    ```

### Running Tests

*   **Unit Tests:**
    ```bash
    # Run all unit tests for the core module
    ./gradlew :skutter-service-core:test

    # Run a specific unit test class
    ./gradlew :skutter-service-core:test --tests "ai.skutter.common.util.SomeUtilTest"
    ```

*   **Integration Tests:** (Requires Docker, configured `.env.integration-test`)
    ```bash
    # Run all integration tests for the core module
    ./gradlew :skutter-service-core:integrationTest

    # Run a specific integration test class
    ./gradlew :skutter-service-core:integrationTest --tests "ai.skutter.common.security.SupabaseJwtIntegrationTest"
    ```
    *See the [Integration Test README](./skutter-service-core/src/integration-test/README.md) for more details on setup.*

### Database Migrations (Flyway)

The `skutter-service-core` project uses Flyway to manage database schema changes.

*   **Migration Files:** SQL migration scripts are located in `skutter-service-core/src/main/resources/db/migration`.
*   **Naming Convention:** Files must follow the pattern `V<VERSION>__<DESCRIPTION>.sql` (e.g., `V1_0__Initial_Setup.sql`). Note the double underscore `__`.
*   **Applying Migrations:** Migrations can be applied in two ways:
    1.  **Automatically on Startup:** Applications using `skutter-service-core` typically apply migrations automatically at startup if `spring.flyway.enabled=true` (the default in this library).
    2.  **Manually via CLI:** Use the Command-Line Interface feature described below.
    3.  **Manually via Gradle:** (Less common for deployed apps, useful during development)
        ```bash
        # Ensure environment variables like SPRING_DATASOURCE_URL/USERNAME/PASSWORD are set
        ./gradlew :skutter-service-core:flywayMigrate
        ```
        *Use `./gradlew :skutter-service-core:tasks --group=Flyway` to see all Gradle Flyway tasks.*

### Command-Line Interface (CLI)

Applications built with `skutter-service-core` include built-in CLI support for common operational tasks, starting with Flyway database migrations.

**Usage (Flyway):**

Run the executable JAR of your application (the one *using* `skutter-service-core`), passing `flyway` followed by the desired Flyway command as arguments:

```bash
# Ensure application properties or environment variables for the database are set
# (e.g., SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD)

# Example using java -jar on an application called 'my-app.jar'
java -jar my-app.jar flyway migrate
java -jar my-app.jar flyway info
java -jar my-app.jar flyway validate
java -jar my-app.jar flyway clean # Warning: Drops DB objects!

# Example within a Docker container entrypoint or command
# docker run ... your-image flyway migrate
```

**How it Works:**

*   The application detects the `flyway <command>` arguments on startup via a `CommandLineRunner`.
*   It uses the existing Spring Boot context to get the configured `DataSource` and `Flyway` beans.
*   It executes the specified Flyway command.
*   It exits with code 0 on success or a non-zero code on failure, **without starting the web server** (if applicable).

**Supported Commands:**

*   `migrate`: Applies pending migrations.
*   `info`: Displays migration status information.
*   `validate`: Validates applied migrations against available ones.
*   `clean`: **Drops all objects** in the configured schemas managed by Flyway. Use with extreme caution.

### Publishing the Library

To publish the `skutter-service-core` library (e.g., to GitHub Packages):

1.  Ensure your `~/.gradle/gradle.properties` or environment variables have the necessary credentials (`GITHUB_ACTOR`, `GITHUB_TOKEN`).
2.  Run the publish command:
    ```bash
    ./gradlew :skutter-service-core:publish
    ```

## Contributing

(Add contribution guidelines here - e.g., branching strategy, code style, pull request process).

## License

(Specify project license - e.g., Apache 2.0)
