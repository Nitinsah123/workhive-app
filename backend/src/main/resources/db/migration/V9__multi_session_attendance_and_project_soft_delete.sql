-- WorkHive V9: Multi-session Attendance & Project Soft Deletion

-- 1. Drop unique constraint on attendance table to allow multiple punch-in / punch-out sessions per day
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'attendance'::regclass
          AND contype = 'u'
    ) LOOP
        EXECUTE 'ALTER TABLE attendance DROP CONSTRAINT IF EXISTS ' || quote_ident(r.conname);
    END LOOP;
END $$;

-- 2. Add soft deletion columns to projects table
ALTER TABLE projects ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE projects ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- 3. Create helpful index for active sessions lookup
CREATE INDEX IF NOT EXISTS idx_attendance_user_date_status ON attendance(tenant_id, user_id, date, status);
CREATE INDEX IF NOT EXISTS idx_projects_tenant_deleted ON projects(tenant_id, is_deleted);
