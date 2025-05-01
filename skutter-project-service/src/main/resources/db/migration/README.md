# Database Migrations - Project Service

**NOTE:** Shared database schema migrations are managed centrally in the `skutter-service-core` module at `skutter-service-core/src/main/resources/db/migration`.

This directory should generally **not** contain Flyway migration scripts that modify the shared schema defined by `skutter-service-core`.

To apply schema changes, run the following command from the **root project directory**:

```bash
../gradlew :skutter-service-core:flywayMigrate
```

If this service requires *completely separate*, service-specific tables or schemas *not* managed by the core library, migrations could potentially be placed here, but would require separate Flyway configuration within this service's build or application properties, distinct from the core schema management. This is **not** the standard setup for schemas shared via `skutter-service-core`.

## Naming Convention

Flyway migrations must follow the naming convention: 