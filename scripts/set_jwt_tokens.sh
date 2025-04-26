#!/bin/bash

echo "Setting up JWT tokens and updating .env.integration-test file..."

# Configuration
SUPABASE_URL="https://klsyvcfpqgcgxiropjve.supabase.co"
SUPABASE_ANON_KEY="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imtsc3l2Y2ZwcWdjZ3hpcm9wanZlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQ5MDk0OTksImV4cCI6MjA2MDQ4NTQ5OX0.yxj7ymHUQEL5Lf-9oQpaUeeyETsmQvoZ9Hj1GOrhxv8"
ENV_FILE=".env.integration-test"

# Utility function for token fetching, exporting and file updating
get_and_update_token() {
    local email=$1
    local password=$2
    local var_name=$3
    local role=$4

    echo "Fetching token for $role ($email)..."
    
    # Fetch the token directly using the known working approach
    local token=$(curl -s -X POST "$SUPABASE_URL/auth/v1/token?grant_type=password" \
        -H "apikey: $SUPABASE_ANON_KEY" \
        -H "Content-Type: application/json" \
        -d "{ \"email\": \"$email\", \"password\": \"$password\" }" \
        | jq -r '.access_token')
    
    # Check if token was fetched successfully
    if [ -z "$token" ] || [ "$token" = "null" ]; then
        echo "ERROR: Failed to fetch token for $email"
        return 1
    fi
    
    # Export the token to the environment
    export "$var_name=$token"
    echo "✓ Exported $var_name to environment"
    
    # Update the .env file if it exists
    if [ -f "$ENV_FILE" ]; then
        # Check if the variable already exists in the file
        if grep -q "^$var_name=" "$ENV_FILE"; then
            # For macOS compatibility, use sed with ''
            sed -i '' "s|^$var_name=.*|$var_name=$token|" "$ENV_FILE"
            echo "✓ Updated $var_name in $ENV_FILE"
        else
            # Add the variable if it doesn't exist
            echo "$var_name=$token" >> "$ENV_FILE"
            echo "✓ Added $var_name to $ENV_FILE"
        fi
    else
        echo "WARN: $ENV_FILE not found, skipping file update"
    fi
}

# Fetch and update tokens
get_and_update_token "platformowner@skutter.ai" "letmein" "JWT_PLATFORM_OWNER_TOKEN" "PLATFORM_OWNER" 
get_and_update_token "platformviewer@skutter.ai" "letmein" "JWT_PLATFORM_VIEWER_TOKEN" "PLATFORM_VIEWER"
get_and_update_token "projectadmin@skutter.ai" "letmein" "JWT_PROJECT_ADMIN_TOKEN" "PROJECT_ADMIN"
get_and_update_token "projectviewer@skutter.ai" "letmein" "JWT_PROJECT_VIEWER_TOKEN" "PROJECT_VIEWER"

# Verify
echo 
echo "--- JWT Tokens (First 30 chars) ---"
echo "JWT_PLATFORM_OWNER_TOKEN: ${JWT_PLATFORM_OWNER_TOKEN:0:30}..."
echo "JWT_PLATFORM_VIEWER_TOKEN: ${JWT_PLATFORM_VIEWER_TOKEN:0:30}..."
echo "JWT_PROJECT_ADMIN_TOKEN: ${JWT_PROJECT_ADMIN_TOKEN:0:30}..."
echo "JWT_PROJECT_VIEWER_TOKEN: ${JWT_PROJECT_VIEWER_TOKEN:0:30}..."
echo
echo "Remember to run this script with 'source ./set_jwt_tokens.sh' to export variables to your current shell"
