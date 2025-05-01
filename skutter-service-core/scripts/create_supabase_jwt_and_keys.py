#!/usr/bin/env python3
"""
Supabase JWT Token Generator

This script generates JWT tokens for Supabase authentication, creating both
anonymous and service role tokens. These tokens are used for API authentication
in the Supabase ecosystem.

Requirements:
    - PyJWT library: pip install PyJWT
"""

import sys
import argparse
from datetime import datetime, timedelta
import secrets
import jwt

def generate_secure_secret(length: int = 32) -> str:
    """Generate a cryptographically secure random string for use as JWT secret."""
    return secrets.token_urlsafe(length)

def calculate_timestamps(validity_years: int = 2) -> tuple[int, int]:
    """
    Calculate JWT timestamp fields (iat, exp) based on current time.
    
    Args:
        validity_years: Number of years the token should be valid for
    
    Returns:
        tuple: (issued_at, expiration) timestamps
    """
    now = datetime.utcnow()
    issued_at = int(now.timestamp())
    expiration = int((now + timedelta(days=365 * validity_years)).timestamp())
    return issued_at, expiration

def create_jwt_token(role: str, secret: str, iat: int, exp: int) -> str:
    """
    Create a JWT token for Supabase authentication.
    
    Args:
        role: The role to encode in the token ('anon' or 'service_role')
        secret: The secret key used to sign the token
        iat: Issued at timestamp
        exp: Expiration timestamp
    
    Returns:
        str: Encoded JWT token
    """
    payload = {
        "role": role,
        "iss": "supabase",
        "iat": iat,
        "exp": exp
    }
    return jwt.encode(payload, secret, algorithm="HS256")

def main():
    """Main execution function."""
    parser = argparse.ArgumentParser(description='Generate Supabase JWT tokens')
    parser.add_argument('--secret', 
                      help='JWT secret key (will generate if not provided)',
                      default=None)
    parser.add_argument('--validity-years',
                      help='Token validity period in years',
                      type=int,
                      default=2)
    parser.add_argument('--output-format',
                      choices=['text', 'env'],
                      default='text',
                      help='Output format (text or env file format)')
    
    args = parser.parse_args()
    
    # Generate or use provided secret
    secret = args.secret or generate_secure_secret()
    
    # Calculate timestamps
    iat, exp = calculate_timestamps(args.validity_years)
    
    # Generate tokens
    anon_token = create_jwt_token('anon', secret, iat, exp)
    service_token = create_jwt_token('service_role', secret, iat, exp)
    
    # Output results based on format
    if args.output_format == 'env':
        print(f"SUPABASE_JWT_SECRET={secret}")
        print(f"SUPABASE_ANON_KEY={anon_token}")
        print(f"SUPABASE_SERVICE_ROLE_KEY={service_token}")
    else:
        print("Generated JWT Tokens:")
        print("-" * 50)
        print(f"JWT Secret: {secret}")
        print(f"Anon Key: {anon_token}")
        print(f"Service Role Key: {service_token}")
        print("\nToken Details:")
        print(f"Valid from: {datetime.fromtimestamp(iat).isoformat()}")
        print(f"Valid until: {datetime.fromtimestamp(exp).isoformat()}")
        print(f"Validity period: {args.validity_years} years")

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
        sys.exit(1)