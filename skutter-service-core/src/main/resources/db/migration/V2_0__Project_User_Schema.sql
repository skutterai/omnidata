
/* ────────────────────────────────
   1. Helpers (Supabase schema exists)
   ──────────────────────────────── */
--CREATE SCHEMA IF NOT EXISTS auth;

/* Optional, keep if some SQL needs the DB role string */
--CREATE OR REPLACE FUNCTION auth.role() RETURNS text
--LANGUAGE plpgsql SECURITY DEFINER AS $$
--BEGIN
--  RETURN current_user;
--END $$;


/* ────────────────────────────────
   2. Project-level roles
   ──────────────────────────────── */
DO $$
BEGIN
    -- Check if the enum type already exists
    IF NOT EXISTS (SELECT 1 FROM pg_type t 
                   JOIN pg_namespace n ON n.oid = t.typnamespace 
                   WHERE t.typname = 'project_role' 
                   AND n.nspname = 'skutter') THEN
        -- Create the enum type if it doesn't exist
        CREATE TYPE skutter.project_role AS ENUM (
            'PROJECT_VIEWER',
            'PROJECT_EDITOR',
            'PROJECT_OWNER'
        );
    END IF;
END
$$;

/* ────────────────────────────────
   3. Projects table
   ──────────────────────────────── */
CREATE TABLE IF NOT EXISTS skutter.projects (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name        citext   NOT NULL CHECK (name <> ''),   -- case-insensitive UNIQUE
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    enabled     boolean      NOT NULL DEFAULT true,

    UNIQUE (name)
);

ALTER TABLE skutter.projects ENABLE ROW LEVEL SECURITY;

/* single BEFORE-UPDATE trigger:
     • rejects changes to immutable columns
     • bumps updated_at with wall-clock time               */
CREATE OR REPLACE FUNCTION skutter.projects_bu()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF OLD.name <> NEW.name THEN
    RAISE EXCEPTION 'Column "name" is immutable';
  END IF;

  NEW.updated_at := clock_timestamp();          -- see explanation below
  RETURN NEW;
END $$;

CREATE TRIGGER skutter_projects_bu
BEFORE UPDATE ON skutter.projects
FOR EACH ROW EXECUTE FUNCTION skutter.projects_bu();

/* ────────────────────────────────
   4. User ↔ project assignments
   ──────────────────────────────── */
CREATE TABLE IF NOT EXISTS skutter.user_project_assignments (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    project_id  uuid NOT NULL REFERENCES skutter.projects(id)    ON DELETE CASCADE,
    role        skutter.project_role NOT NULL DEFAULT 'PROJECT_VIEWER',
    assigned_at timestamptz  NOT NULL DEFAULT now(),
    enabled     boolean      NOT NULL DEFAULT true,

    UNIQUE (user_id, project_id)
);

/* Covering index for the RLS predicate */
CREATE INDEX skutter_upa_lookup
ON skutter.user_project_assignments (project_id, user_id)
WHERE enabled;

/* ────────────────────────────────
   5. Row-level-security policies
   ──────────────────────────────── */
CREATE POLICY project_access_policy ON skutter.projects
USING (
  EXISTS (
    SELECT 1
    FROM skutter.user_project_assignments upa
    WHERE upa.project_id = skutter.projects.id
      AND upa.user_id    = auth.uid()          -- Supabase helper  :contentReference[oaicite:2]{index=2}
      AND upa.enabled
  )
  AND skutter.projects.enabled
);

-- (Add complementary WITH CHECK policies as needed)

/* Optional partial index if many projects are disabled
CREATE INDEX skutter.projects_enabled_idx ON skutter.projects(id) WHERE enabled; */
