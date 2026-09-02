-- =====================================================
-- WorkHive Database Schema - V6 Outbox Events Payload Type Fix
-- =====================================================

-- Change payload column from JSONB to TEXT to match OutboxEvent JPA entity (Types#VARCHAR)
ALTER TABLE outbox_events ALTER COLUMN payload TYPE TEXT USING payload::text;
