# HTTPS Configuration Guide

This guide explains how to configure HTTPS for your Skutter Service.

## Overview

The Skutter Service Core supports HTTPS out of the box. It provides the following features:

- Secure communication using TLS/SSL
- Enforcement of HTTPS for all requests
- Support for HTTP Strict Transport Security (HSTS)
- Customizable keystore configuration

## Configuration

### 1. Basic Configuration

Add the following to your `application.yml` file:

```yaml
skutter:
  security:
    https:
      enabled: true
      port: 8443
      keystore-path: classpath:keystore/keystore.p12
      keystore-password: your-keystore-password
      keystore-type: PKCS12
      keystore-alias: your-key-alias
```

### 2. HSTS Configuration

HSTS (HTTP Strict Transport Security) is a security feature that helps protect against protocol downgrade attacks and cookie hijacking. Add the following to your `application.yml`:

```yaml
skutter:
  security:
    https:
      hsts-enabled: true
      hsts-max-age-seconds: 31536000  # 1 year
      hsts-include-sub-domains: true
      hsts-preload: false
```

### 3. Environment Variables

You can also configure HTTPS using environment variables:

```
SKUTTER_HTTPS_ENABLED=true
SKUTTER_HTTPS_PORT=8443
SKUTTER_HTTPS_KEYSTORE_PATH=classpath:keystore/keystore.p12
SKUTTER_HTTPS_KEYSTORE_PASSWORD=your-keystore-password
SKUTTER_HTTPS_KEYSTORE_TYPE=PKCS12
SKUTTER_HTTPS_KEYSTORE_ALIAS=your-key-alias
SKUTTER_HTTPS_HSTS_ENABLED=true
SKUTTER_HTTPS_HSTS_MAX_AGE_SECONDS=31536000
SKUTTER_HTTPS_HSTS_INCLUDE_SUB_DOMAINS=true
SKUTTER_HTTPS_HSTS_PRELOAD=false
```

## Creating a Certificate

### For Development/Testing

For development and testing, you can use a self-signed certificate. A script is provided in the `scripts` directory:

```bash
cd scripts
chmod +x generate-self-signed-cert.sh
./generate-self-signed-cert.sh
```

This will create a self-signed certificate in `src/main/resources/keystore/keystore.p12`.

> **WARNING**: Self-signed certificates should **never** be used in production.

### For Production

For production, you should use a certificate from a trusted Certificate Authority (CA). Here's how to create a keystore with a CA-issued certificate:

1. Generate a private key and CSR (Certificate Signing Request):

```bash
openssl req -newkey rsa:2048 -nodes -keyout private.key -out request.csr
```

2. Submit the CSR to your CA and receive the signed certificate.

3. Convert the private key and certificate to PKCS12 format:

```bash
openssl pkcs12 -export -in certificate.crt -inkey private.key -out keystore.p12 -name your-key-alias
```

4. Place the resulting `keystore.p12` file in your `src/main/resources/keystore` directory.

## Running with a Reverse Proxy

If you're running the application behind a reverse proxy (like Nginx, Apache, or a cloud load balancer), make sure to configure the proxy to forward the appropriate headers:

```yaml
server:
  forward-headers-strategy: native
```

This allows Spring Boot to correctly determine the client's original protocol, address, and port.

## Troubleshooting

### Certificate Issues

If you encounter certificate-related issues, you can use the following command to verify your keystore:

```bash
keytool -list -v -keystore keystore.p12 -storetype PKCS12
```

### SSL Handshake Errors

If clients are having trouble connecting, verify that:

1. The certificate is valid and not expired
2. The certificate's CN (Common Name) or SAN (Subject Alternative Name) matches the domain
3. The client trusts the certificate or the CA that issued it

## Security Considerations

1. Always use strong passwords for your keystores
2. Rotate certificates before they expire
3. Consider using stronger key sizes (at least 2048 bits for RSA)
4. Enable HSTS to prevent protocol downgrade attacks
5. Keep your JDK/JRE updated to get the latest security patches 