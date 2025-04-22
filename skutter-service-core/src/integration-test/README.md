# Skutter Service Core Integration Tests

This directory contains integration tests that verify the functionality of the Skutter Service Core when integrated with external services like Supabase.

## Supabase JWT Integration Test

The `SupabaseJwtIntegrationTest` verifies that our JWT processing logic works correctly with real Supabase tokens. It tests token validation, role extraction, and permission assignment for all four Skutter platform roles:

- PLATFORM_OWNER
- PLATFORM_VIEWER
- PROJECT_ADMIN
- PROJECT_VIEWER

### Prerequisites

To run these tests, you need:

1. A Supabase project with auth configured
2. User accounts set up in Supabase with the correct roles in their `app_metadata`
3. Environment variables configured with connection details and credentials

### Environment Variables

Set the following environment variables before running the tests:

```
# Supabase Connection Details
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_API_KEY=your-supabase-api-key

# JWT Configuration - REQUIRED for token validation
JWT_SECRET=your-supabase-jwt-secret-key
JWT_ISSUER=https://your-project.supabase.co/auth/v1

# User with PLATFORM_OWNER role
SUPABASE_EMAIL_PLATFORM_OWNER=platformowner@example.com
SUPABASE_PASSWORD_PLATFORM_OWNER=password

# User with PLATFORM_VIEWER role
SUPABASE_EMAIL_PLATFORM_VIEWER=platformviewer@example.com
SUPABASE_PASSWORD_PLATFORM_VIEWER=password

# User with PROJECT_ADMIN role
SUPABASE_EMAIL_PROJECT_ADMIN=projectadmin@example.com
SUPABASE_PASSWORD_PROJECT_ADMIN=password

# User with PROJECT_VIEWER role
SUPABASE_EMAIL_PROJECT_VIEWER=projectviewer@example.com
SUPABASE_PASSWORD_PROJECT_VIEWER=password
```

The `JWT_SECRET` is particularly important - it must match the JWT secret configured in your Supabase project settings (found under Project Settings > API > JWT Settings). Without this, token validation will fail.

A template file with these variables is available at `src/integration-test/resources/env-integration-test.template`. Copy this file to `.env.integration-test` in your project root and update the values:

```bash
cp src/integration-test/resources/env-integration-test.template .env.integration-test
```

Then edit the `.env.integration-test` file with your actual credentials.

### Setting Up User Roles in Supabase

1. Create users in your Supabase auth system
2. For each user, set the `app_metadata` to include the `skutter_role`:

```json
{
  "skutter_role": "PLATFORM_OWNER"
}
```

Use the appropriate role value for each user: `PLATFORM_OWNER`, `PLATFORM_VIEWER`, `PROJECT_ADMIN`, or `PROJECT_VIEWER`.

### Running the Tests

You can run the tests using Gradle:

```bash
# From the project root
./gradlew :skutter-service-core:integrationTest
```

or from your IDE by running the `SupabaseJwtIntegrationTest` class directly (after disabling the `@Disabled` annotation).

### Troubleshooting

If the tests fail, check:

1. The environment variables are set correctly
2. The `JWT_SECRET` matches the one in your Supabase project settings
3. The Supabase project is accessible
4. User credentials are valid
5. Users have the correct roles assigned in their `app_metadata`
6. The JWT issuer matches your Supabase auth endpoint

Log output will help identify specific issues with token acquisition or validation. 