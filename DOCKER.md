# Docker Support for Omnidata

This project includes Docker support for both development and production environments, making it easy to build and run containers for the application and its dependencies.

## Project Structure

This repository is organized as a multi-service project:
- Each service has its own Dockerfile in its directory (e.g., `skutter-service-core/Dockerfile`)
- Shared infrastructure (database, etc.) is configured at the root level
- The docker-compose.yml file at the root orchestrates all services

## Prerequisites

- Docker and Docker Compose installed on your system
- JDK 17 for local development (outside Docker)

## Configuration

1. Create a `.env` file based on the provided `.env-template`:

```bash
cp .env-template .env
```

2. Edit the `.env` file to configure your database credentials and other settings.

## Running with Docker Compose

### Start the complete stack:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database with PostGIS extensions
- Database preparation service
- All application services

### Start only the database for development:

```bash
docker-compose up -d postgres db-prep
```

This will start only the database components, allowing you to run services locally for development.

### Start a specific service:

```bash
docker-compose up -d skutter-service-core
```

### View logs:

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f skutter-service-core
```

### Stop all services:

```bash
docker-compose down
```

## Building with JIB

The project uses Google's JIB for efficient container builds:

```bash
# Build to local Docker daemon
./gradlew :skutter-service-core:jibDockerBuild

# Build and push to registry
./gradlew :skutter-service-core:jib
```

## Adding a New Service

To add a new service to this project:

1. Create a new directory for your service
2. Add a Dockerfile in that directory
3. Add a new service section in the root docker-compose.yml
4. Configure JIB in your service's build.gradle file

## Integration Testing with Docker

For integration testing with a live database:

1. Start the database:

```bash
docker-compose up -d postgres db-prep
```

2. Run integration tests:

```bash
./gradlew :skutter-service-core:integrationTest
```

## Customizing Database Setup

The database initialization is handled by `scripts/create-skutter-db.sh`. You can modify this script to add additional database setup steps, create test data, or configure specific PostgreSQL extensions. 