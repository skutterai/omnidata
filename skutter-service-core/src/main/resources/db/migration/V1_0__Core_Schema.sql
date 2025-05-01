/* ────────────────────────────────
   0. Extensions ( keep it lean )
   ──────────────────────────────── */
CREATE EXTENSION IF NOT EXISTS postgis;         -- spatial types & operators
CREATE EXTENSION IF NOT EXISTS citext;          -- case-insensitive text  :contentReference[oaicite:0]{index=0}
CREATE EXTENSION IF NOT EXISTS pg_trgm;         -- trigram search support
CREATE EXTENSION IF NOT EXISTS unaccent;        -- strip diacritics

-- `gen_random_uuid()` is built-in from PostgreSQL 13 onward  :contentReference[oaicite:1]{index=1}
-- If you’re on < 13, uncomment the next line
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;

/* ────────────────────────────────
   1. Helpers (Supabase schema exists)
   ──────────────────────────────── */
-- First, ensure the schema exists
CREATE SCHEMA IF NOT EXISTS skutter;