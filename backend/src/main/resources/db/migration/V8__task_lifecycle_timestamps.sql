-- =====================================================
-- WorkHive Database Schema - V8 Task Lifecycle Timestamps
-- Real audit lifecycle timestamps for task transitions
-- =====================================================

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS started_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE;
