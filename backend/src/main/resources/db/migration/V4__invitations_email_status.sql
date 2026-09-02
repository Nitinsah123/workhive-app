-- =====================================================
-- WorkHive Database Schema - V4 Invitations Email Status & Delivery Columns
-- =====================================================

-- 1. Add email_status column required by Hibernate schema validation on Invitation entity
ALTER TABLE invitations ADD COLUMN IF NOT EXISTS email_status VARCHAR(30) NOT NULL DEFAULT 'EMAIL_PENDING';

-- 2. Add accompanying delivery tracking columns defined on Invitation entity
ALTER TABLE invitations ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE invitations ADD COLUMN IF NOT EXISTS sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE invitations ADD COLUMN IF NOT EXISTS error_message VARCHAR(1000);

-- 3. Safely backfill any existing historical rows to satisfy NOT NULL constraint
UPDATE invitations SET email_status = 'EMAIL_PENDING' WHERE email_status IS NULL;

-- 4. Set previously accepted invitations to EMAIL_SENT if they were in default state
UPDATE invitations SET email_status = 'EMAIL_SENT' WHERE status = 'ACCEPTED' AND email_status = 'EMAIL_PENDING';

-- 5. Add performance index for tenant-scoped email status lookups
CREATE INDEX IF NOT EXISTS idx_invitations_tenant_email_status ON invitations(tenant_id, email_status);
