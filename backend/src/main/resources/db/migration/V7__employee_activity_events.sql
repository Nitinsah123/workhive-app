-- =====================================================
-- WorkHive Database Schema - V7 Employee Activity Events
-- Real-Time Department & Team Member Roster + 360 Activity Telemetry
-- =====================================================

CREATE TABLE IF NOT EXISTS employee_activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    department_id UUID REFERENCES departments(id) ON DELETE SET NULL,
    team_id UUID REFERENCES teams(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL, -- GIT_PUSH, GIT_PR, TASK_ASSIGNED, TASK_COMPLETED, HEARTBEAT
    title VARCHAR(500),
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_emp_act_events_tenant_user ON employee_activity_events(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_emp_act_events_tenant_dept ON employee_activity_events(tenant_id, department_id);
CREATE INDEX IF NOT EXISTS idx_emp_act_events_tenant_team ON employee_activity_events(tenant_id, team_id);
CREATE INDEX IF NOT EXISTS idx_emp_act_events_tenant_created ON employee_activity_events(tenant_id, created_at DESC);
