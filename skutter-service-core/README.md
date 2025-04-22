# Skutter Common Framework Library

A production-ready Spring Boot library that serves as a shared dependency for microservices. This library provides standardized components for security, API handling, database access, observability, and more.

## Features

### Security
- Complete OAuth2 integration with Supabase, focusing on JWT processing
- Security logging filter for debugging
- Postgres user ID propagation via SET LOCAL
- Custom JWT claim extraction for `skutter_role` from `app_metadata`
- Role-based authorization utilities
- Certificate loading and hot-reloading mechanisms

### REST API
- Standardized error handling with global exception advisors
- Rate limiting with configurable strategies
- Circuit breaker patterns using Resilience4j
- Response compression and caching strategies
- Comprehensive OpenAPI/Swagger documentation

### Data Access
- JPA configuration with connection pooling best practices
- PostgreSQL specific optimizations
- PostGIS support with proper type handling
- Database migration support with Flyway

### Observability
- Correlation ID propagation across services
- Prometheus metrics exporters
- Spring Boot Actuator extensions
- Health check interfaces
- MDC logging support

### Utilities
- Environment variable configuration
- Null-safety patterns
- Date/time handling utilities with timezone awareness
- Encryption helpers
- Response object builders

## Getting Started

### Prerequisites
- Java 17 or higher
- Gradle 8.x

### Installation

Add the following to your `build.gradle`:

```groovy
repositories {
    mavenCentral()
    maven {
        name = 'GitHubPackages'
        url = uri('https://maven.pkg.github.com/skutter/skutter-service-core')
        credentials {
            username = project.findProperty('gpr.user') ?: System.getenv('GITHUB_USERNAME')
            password = project.findProperty('gpr.key') ?: System.getenv('GITHUB_TOKEN')
        }
    }
}

dependencies {
    implementation 'ai.skutter.common:skutter-common-framework:0.1.0-SNAPSHOT'
}
```

### Configuration

The library uses Spring Boot's auto-configuration, so most components are automatically configured. You can customize behavior with the following properties in your `application.yml`:

```yaml
skutter:
  security:
    enabled: true
    jwt:
      secret: your-jwt-secret-here
      # OR
      public-key-path: classpath:public-key.pem
      issuer: https://api.supabase.co/auth/v1
      validate-expiration: true
      validate-issuer: true
      role-claim: app_metadata.skutter_role
      set-postgres-user-id: true
    public-paths:
      - /actuator/health/**
      - /v3/api-docs/**
  
  api:
    rate-limit:
      enabled: true
      limit: 100
      refresh-period: 1m
    documentation:
      title: Your API Title
      version: 1.0.0
      description: API Description
    resilience:
      circuit-breaker-enabled: true
      failure-threshold: 50
  
  data:
    enable-user-id-propagation: true
    flyway:
      enabled: true
      locations: classpath:db/migration
    postgis:
      enabled: true
      default-srid: 4326
  
  observability:
    correlation:
      header-name: X-Correlation-ID
      generate-if-missing: true
    metrics:
      application-name: your-application-name
    logging:
      include-correlation-id: true
```

## Usage Examples

### Security - Setting up Supabase Authentication

```java
@RestController
@RequestMapping("/api")
public class SecuredController {

    @GetMapping("/private")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<String> privateEndpoint(Authentication auth) {
        SupabaseUserDetails userDetails = (SupabaseUserDetails) auth.getPrincipal();
        String userId = userDetails.getUserId();
        return ResponseEntity.ok("Hello, " + userId);
    }
}
```

### API - Adding Rate Limiting

Rate limiting is automatically applied to all endpoints. You can customize it in your application.yml:

```yaml
skutter:
  api:
    rate-limit:
      enabled: true
      limit: 100  # requests per refresh period
      refresh-period: 1m  # 1 minute
```

### Database - Setting up Flyway Migrations

```yaml
skutter:
  data:
    flyway:
      enabled: true
      locations: classpath:db/migration
      baseline-on-migrate: true
```

### Observability - Correlation ID Tracking

Correlation IDs are automatically added to all requests and responses, and are available in logs through MDC:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class YourService {
    private static final Logger log = LoggerFactory.getLogger(YourService.class);
    
    public void doSomething() {
        // The correlation ID will automatically be included in the log output
        log.info("Processing something important");
    }
}
```

## Publishing

To publish the library to GitHub Packages:

```shell
export GITHUB_ACTOR=your-github-username
export GITHUB_TOKEN=your-github-token
./gradlew publish
```

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request 