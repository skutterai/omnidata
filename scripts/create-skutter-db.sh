#!/bin/sh
set -e

# Setup logging
LOG_FILE="/var/log/create-db.log"
mkdir -p "$(dirname $LOG_FILE)"
touch $LOG_FILE

# Logging function
log() {
  local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
  echo "[$timestamp] $1" | tee -a $LOG_FILE
}

# Extract database connection info from environment variables
DB_HOST="postgres"
DB_USER="${SPRING_DATASOURCE_USERNAME:-postgres}"
DB_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-your_secure_password}"
DB_NAME="${SKUTTER_DB:-skutter}"

log "Starting database setup process"
log "Checking for PostgreSQL availability on $DB_HOST..."
log "User: $DB_USER Password: $DB_PASSWORD"
export PGPASSWORD="$DB_PASSWORD"

# Wait for PostgreSQL to be up
until psql -h "$DB_HOST" -U "$DB_USER" -d "postgres" -c '\q' > /dev/null 2>&1; do
  log "PostgreSQL is unavailable - sleeping"
  sleep 2
done

log "PostgreSQL is available. Checking if $DB_NAME database exists..."

# Check if the database already exists
if psql -h "$DB_HOST" -U "$DB_USER" -d "postgres" -t -c "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME';" | grep -q 1; then
  log "Database $DB_NAME already exists."
else
  log "Creating $DB_NAME database with en_GB.utf8 locale..."
  
  # Create the database with proper locale settings
  psql -h "$DB_HOST" -U "$DB_USER" -d "postgres" -c "CREATE DATABASE $DB_NAME WITH ENCODING='UTF8' LC_COLLATE='en_GB.utf8' LC_CTYPE='en_GB.utf8' TEMPLATE=template0;" 2>&1 | tee -a $LOG_FILE
  
  log "Created database $DB_NAME."
  
  # Connect to the new database and enable extensions
  log "Enabling PostGIS and other extensions..."
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS postgis;" 2>&1 | tee -a $LOG_FILE
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS postgis_topology;" 2>&1 | tee -a $LOG_FILE
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;" 2>&1 | tee -a $LOG_FILE
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS postgis_tiger_geocoder;" 2>&1 | tee -a $LOG_FILE
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;" 2>&1 | tee -a $LOG_FILE
  psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS unaccent;" 2>&1 | tee -a $LOG_FILE
  
  log "All extensions enabled in $DB_NAME database."
fi

# Create a table to mark database initialization
log "Creating initialization marker..."
psql -h "$DB_HOST" -U "$DB_USER" -d "$DB_NAME" -c "
CREATE TABLE IF NOT EXISTS db_init_info (
  id SERIAL PRIMARY KEY,
  initialized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  version TEXT,
  initialized_by TEXT
);
INSERT INTO db_init_info (version, initialized_by) 
VALUES ('1.0', 'docker-db-prep');" 2>&1 | tee -a $LOG_FILE

# Create a flag file to indicate completion
touch /tmp/db_created

log "Database setup complete. The Spring application can now connect to the $DB_NAME database."
log "Flyway migrations will be handled by your application." 