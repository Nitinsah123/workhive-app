-- =====================================================
-- WorkHive Database Schema - V2 Email Connections
-- =====================================================

CREATE TABLE email_connections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    user_id UUID NOT NULL REFERENCES users(id),
    provider VARCHAR(30) NOT NULL DEFAULT 'GMAIL',
    email_address VARCHAR(255),
    access_token_enc TEXT,
    refresh_token_enc TEXT,
    token_expires_at TIMESTAMP WITH TIME ZONE,
    scopes VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_CONNECTED',
    oauth_state VARCHAR(255),
    last_send_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_email_conn_tenant_user_provider UNIQUE(tenant_id, user_id, provider)
);

CREATE INDEX idx_email_conn_tenant ON email_connections(tenant_id);
CREATE INDEX idx_email_conn_tenant_user ON email_connections(tenant_id, user_id);
CREATE INDEX idx_email_conn_oauth_state ON email_connections(oauth_state);
