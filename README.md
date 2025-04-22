# Skutter Service Core

A production-ready Spring Boot library project (`skutter-service-core`) that serves as a shared dependency for Skutter microservices. It provides common configurations, utilities, and base classes to accelerate development and ensure consistency across services.

## Features

This library includes reusable components for:

*   **Security:**
    *   JWT validation integrated with Supabase Auth.
    *   Role-based access control (`SkutterRole`) and method security expressions (`@PreAuthorize` using custom logic like `isPlatformOwner()`).
    *   Standardized CORS configuration.
    *   Optional HTTPS enforcement and HSTS configuration.
    *   Filters for JWT authentication and security logging.
*   **Observability:**
    *   Correlation ID generation and propagation (`CorrelationIdFilter`).
    *   Standardized metrics configuration with common tags.
    *   Logging configuration utilities and Actuator endpoint (`LoggingController`).
*   **Data Access:**
    *   JPA Auditing setup (`@CreatedBy`, `@LastModifiedBy`, etc.).
    *   Optional PostgreSQL interceptor to set `user_id` context.
    *   Flyway configuration integration.
    *   Spatial data type support (`org.hibernate.orm:hibernate-spatial`).
*   **API Handling:**
    *   Global exception handling (`GlobalExceptionHandler`).
    *   Rate limiting support (`RateLimitInterceptor`).
    *   Resilience patterns using Resilience4j (Circuit Breaker, Retry).
    *   Standardized pagination models (`CursorPageRequest`, `OffsetPageRequest`, `PagedResponse`).
    *   Standardized error response format (`ErrorResponse`).
*   **Utilities:**
    *   Deterministic ID Generation (`DeterministicIdGenerator`): Creates stable, short IDs from various inputs.
    *   Type Identification (`TypeIdentifier`): Identifies common data types like emails, URLs, IPs, etc.
    *   Memory/Data Size Utilities (`DataSize`, `DataSizeUnit`, `CheckMemoryLimit`).
    *   Common Apache Commons and Guava utilities.

## Building the Library

This project uses Gradle.

1.  **Prerequisites:**
    *   JDK 17 (Adoptium recommended)
    *   Gradle (uses wrapper, `./gradlew`)

2.  **Build:** Navigate to the project root directory (`omnidata`) and run:
    ```bash
    ./gradlew build
    ```
    This command compiles the code, runs unit tests, performs Checkstyle checks, and generates the JAR file.

3.  **Run Tests Only:**
    ```bash
    ./gradlew :skutter-service-core:test
    ```

4.  **Run Specific Test:**
    ```bash
    ./gradlew :skutter-service-core:test --tests "ai.skutter.common.util.DeterministicIdGeneratorTest"
    ```

5.  **Generate Javadoc & Sources JARs:** The build automatically generates these.

## Using the Library

To use this library in another Gradle-based Skutter microservice:

1.  **Ensure the library is published:** The library needs to be built and published to a repository accessible by the consuming service (e.g., GitHub Packages, Maven Central, or a local repository).

2.  **Add the dependency:** In the `build.gradle` file of the consuming microservice, add the following dependency:
    ```gradle
    dependencies {
        implementation 'ai.skutter.common:skutter-service-core:<version>'
        // Replace <version> with the desired version, e.g., '0.1.0-SNAPSHOT'
    }
    ```

3.  **Configure Auto-Configuration:** Spring Boot's auto-configuration should automatically pick up the configurations defined in `skutter-service-core` (found under `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`). You can customize behavior using properties defined in the various `@ConfigurationProperties` classes (e.g., `SkutterSecurityProperties`, `SkutterApiProperties`).

## Configuration Properties

Key configuration properties are grouped under prefixes:

*   `skutter.security.*` (See `SkutterSecurityProperties`)
*   `skutter.api.*` (See `SkutterApiProperties`)
*   `skutter.data.*` (See `SkutterDataProperties`)
*   `skutter.observability.*` (See `SkutterObservabilityProperties`)
*   `skutter.actuator.*` (See `SkutterActuatorProperties`)

Refer to the specific properties classes for detailed options.

## Contributing

(Add contribution guidelines here if applicable - e.g., branching strategy, code style, pull request process).
