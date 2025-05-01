# Database Migrations (Flyway) - Skutter Core Schema

## Single Source of Truth

This directory contains the **canonical source** for all shared database schema migrations managed by Flyway for the `skutter-service-core` library and its dependent services (like `skutter-project-service`).

**DO NOT** place shared schema migrations in other modules.

## Naming Convention

Migrations MUST follow the Flyway naming convention:

`V<VERSION>__<Description>.sql`

*   `V`: The prefix (case-sensitive).
*   `<VERSION>`: Underscores (`_`) or dots (`.`) separate version numbers (e.g., `1`, `1_0`, `1_1`, `2_0_1`). Versions are sorted numerically.
*   `__`: Double underscore separator (required).
*   `<Description>`: Underscores separate words (e.g., `Create_initial_schema`, `Add_user_table`).
*   `.sql`: The suffix.

Example: `V1_0__Create_core_tables.sql`

## Applying Migrations

Migrations are applied using the Gradle task defined in the `skutter-service-core` module:

```bash
./gradlew :skutter-service-core:flywayMigrate
```

Ensure the database connection details used by this task (configured in `skutter-service-core/build.gradle`, likely via `.env.integration-test` or environment variables) point to the target database and have sufficient permissions.

## Runtime Validation

Dependent services using `skutter-service-core` typically validate the database schema against these migrations at startup using Spring Boot's Flyway integration.