/*
 * Copyright (c) 2025 Skutter.ai
 *
 * This code is proprietary and confidential. Unauthorized copying, modification,
 * distribution, or use of this software, via any medium is strictly prohibited.
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * @author mattduggan
 */

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Create auth schema if it doesn't exist (usually created by Supabase)
CREATE SCHEMA IF NOT EXISTS auth;

CREATE OR REPLACE FUNCTION auth.role()
RETURNS text AS $$
BEGIN
    RETURN current_user;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create projects table
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    
    CONSTRAINT projects_name_unique UNIQUE (name),
    CONSTRAINT projects_name_not_empty CHECK (LENGTH(TRIM(name)) > 0)
);

ALTER TABLE projects ENABLE ROW LEVEL SECURITY;

COMMENT ON TABLE projects IS 'Stores project information for multi-project application';
COMMENT ON COLUMN projects.id IS 'Unique identifier for the project';
COMMENT ON COLUMN projects.name IS 'Unique lowercase name used for identification (e.g., in URLs)';
COMMENT ON COLUMN projects.created_at IS 'Timestamp when the project was created';
COMMENT ON COLUMN projects.updated_at IS 'Timestamp when the project was last updated';
COMMENT ON COLUMN projects.enabled IS 'Whether the project is currently active';


-- Prevent updates to immutable columns
CREATE OR REPLACE FUNCTION prevent_projects_column_update()
RETURNS TRIGGER AS $$
BEGIN

    IF OLD.name <> NEW.name THEN
        RAISE EXCEPTION 'Column "name" cannot be updated after creation';
    END IF;
    
    IF OLD.created_at <> NEW.created_at THEN
        RAISE EXCEPTION 'Column "created_at" cannot be updated after creation';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Prevent updates to immutable columns
CREATE TRIGGER protect_projects_immutable_fields
BEFORE UPDATE ON projects
FOR EACH ROW
EXECUTE FUNCTION prevent_projects_column_update();


-- Trigger to update the updated_at column
CREATE OR REPLACE FUNCTION update_projects_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to update the updated_at column
CREATE TRIGGER update_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW
    EXECUTE FUNCTION update_projects_updated_at_column();

-- Create user_project_assignments table to track user assignments to projects
CREATE TABLE IF NOT EXISTS user_project_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL, -- from Supabase auth.users
    project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT 'PROJECT_VIEWER', -- optional metadata
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT unique_user_project UNIQUE (user_id, project_id)
);

-- Get the current user's ID
CREATE FUNCTION current_user_id() RETURNS uuid AS $$
BEGIN
  RETURN current_setting('skutter.app.current_user_id', true)::uuid;
END;
$$ LANGUAGE plpgsql STABLE;

-- Allow access to projects based on user assignments
CREATE POLICY project_access_policy ON projects
USING (
    EXISTS (
        SELECT 1 FROM user_project_assignments upa
        WHERE upa.project_id = projects.id
          AND upa.user_id = current_user_id()
          AND upa.enabled = TRUE
    )
    AND projects.enabled = TRUE
);
