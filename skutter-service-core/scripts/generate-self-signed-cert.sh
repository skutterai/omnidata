#!/bin/bash

# Script to generate a self-signed certificate for testing HTTPS

# Variables
KEYSTORE_FILE="keystore.p12"
KEYSTORE_PASSWORD="changeit"
KEYSTORE_ALIAS="skutter"
VALIDITY_DAYS=3650 # 10 years
KEYSTORE_DIR="../src/main/resources/keystore"

# Create output directory if it doesn't exist
mkdir -p $KEYSTORE_DIR

# Generate self-signed certificate and key
keytool -genkeypair \
  -alias $KEYSTORE_ALIAS \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore $KEYSTORE_DIR/$KEYSTORE_FILE \
  -validity $VALIDITY_DAYS \
  -storepass $KEYSTORE_PASSWORD \
  -dname "CN=localhost, OU=Skutter, O=Skutter AI, L=Bishops Stortford, ST=Hertfordshire, C=GB" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"

# Check if keystore was created successfully
if [ $? -eq 0 ]; then
  echo "Successfully generated self-signed certificate at $KEYSTORE_DIR/$KEYSTORE_FILE"
  echo "Password: $KEYSTORE_PASSWORD"
  echo "Alias: $KEYSTORE_ALIAS"
  echo ""
  echo "For production, use a certificate from a trusted certificate authority."
else
  echo "Failed to generate self-signed certificate."
  exit 1
fi

# Print keystore information
keytool -list -v -keystore $KEYSTORE_DIR/$KEYSTORE_FILE -storepass $KEYSTORE_PASSWORD

echo ""
echo "IMPORTANT: This is a self-signed certificate for testing only."
echo "For production, use a certificate from a trusted certificate authority."
echo ""
echo "Add the following configuration to your application.yml:"
echo ""
echo "skutter:"
echo "  security:"
echo "    https:"
echo "      enabled: true"
echo "      port: 8443"
echo "      keystore-path: classpath:keystore/keystore.p12"
echo "      keystore-password: $KEYSTORE_PASSWORD"
echo "      keystore-type: PKCS12"
echo "      keystore-alias: $KEYSTORE_ALIAS" 