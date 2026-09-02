-- =====================================================
-- WorkHive Database Schema - V3 User Scoped Integrations
-- =====================================================

-- Allow each authorized employee to connect their own provider account
ALTER TABLE integrations DROP CONSTRAINT IF EXISTS integrations_tenant_id_provider_key;
ALTER TABLE integrations DROP CONSTRAINT IF EXISTS uk_integrations_tenant_provider;

-- Add user-scoped unique constraint
ALTER TABLE integrations ADD CONSTRAINT uk_integrations_tenant_user_provider UNIQUE (tenant_id, connected_by, provider);
