-- =====================================================
-- WorkHive Database Schema - V5 Org Settings & Task Submissions
-- =====================================================

-- 1. Add setting_value column to org_settings table
ALTER TABLE org_settings ADD COLUMN IF NOT EXISTS setting_value TEXT NOT NULL DEFAULT '{}';

-- Safely backfill setting_value from old value column if it exists and has content
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'org_settings' AND column_name = 'value'
    ) THEN
        UPDATE org_settings SET setting_value = value::text WHERE (setting_value = '{}' OR setting_value IS NULL) AND value IS NOT NULL;
    END IF;
END $$;

-- 2. Create task_submissions table for task pull requests and git commit review workflow
CREATE TABLE IF NOT EXISTS task_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    task_id UUID NOT NULL REFERENCES tasks(id),
    project_id UUID REFERENCES projects(id),
    submitted_by UUID NOT NULL REFERENCES users(id),
    repository_url VARCHAR(1000) NOT NULL,
    provider VARCHAR(50) DEFAULT 'GITHUB',
    external_repository_id VARCHAR(200),
    branch VARCHAR(200),
    pull_request_url VARCHAR(1000),
    commit_sha VARCHAR(100),
    work_summary VARCHAR(3000),
    review_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    review_comment VARCHAR(3000),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    version INTEGER DEFAULT 1,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_task_submissions_tenant ON task_submissions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_task_submissions_task ON task_submissions(task_id);
CREATE INDEX IF NOT EXISTS idx_task_submissions_project ON task_submissions(project_id);
CREATE INDEX IF NOT EXISTS idx_task_submissions_submitted_by ON task_submissions(submitted_by);
