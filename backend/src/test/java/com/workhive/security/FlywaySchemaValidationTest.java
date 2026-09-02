package com.workhive.security;

import com.workhive.module.user.entity.Invitation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FlywaySchemaValidationTest {

    @Test
    @DisplayName("Verify V4 migration file exists and contains correct PostgreSQL DDL for invitations.email_status")
    void testV4MigrationFileContent() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/db/migration/V4__invitations_email_status.sql")) {
            assertNotNull(is, "V4__invitations_email_status.sql must exist on the classpath");
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(content.contains("ALTER TABLE invitations ADD COLUMN IF NOT EXISTS email_status"),
                    "Must add email_status column");
            assertTrue(content.contains("VARCHAR(30)"),
                    "email_status must be VARCHAR(30)");
            assertTrue(content.contains("NOT NULL DEFAULT 'EMAIL_PENDING'"),
                    "email_status must have NOT NULL DEFAULT 'EMAIL_PENDING'");
            assertTrue(content.contains("UPDATE invitations SET email_status = 'EMAIL_PENDING' WHERE email_status IS NULL"),
                    "Must safely backfill existing rows");
        }
    }

    @Test
    @DisplayName("Verify V5 migration file exists and contains correct PostgreSQL DDL for org_settings and task_submissions")
    void testV5MigrationFileContent() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/db/migration/V5__org_settings_and_task_submissions.sql")) {
            assertNotNull(is, "V5__org_settings_and_task_submissions.sql must exist on the classpath");
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(content.contains("ALTER TABLE org_settings ADD COLUMN IF NOT EXISTS setting_value"),
                    "Must add setting_value column to org_settings");
            assertTrue(content.contains("CREATE TABLE IF NOT EXISTS task_submissions"),
                    "Must create task_submissions table");
        }
    }

    @Test
    @DisplayName("Verify V6 migration file exists and alters outbox_events payload to TEXT")
    void testV6MigrationFileContent() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/db/migration/V6__outbox_events_payload_to_text.sql")) {
            assertNotNull(is, "V6__outbox_events_payload_to_text.sql must exist on the classpath");
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            assertTrue(content.contains("ALTER TABLE outbox_events ALTER COLUMN payload TYPE TEXT"),
                    "Must alter payload column to TEXT");
        }
    }

    @Test
    @DisplayName("Verify V4 migration executes on invitations table and satisfies Invitation entity fields")
    void testV4MigrationExecutionAndBackfill() throws Exception {
        String dbName = "invitations_v4_test_" + System.currentTimeMillis();
        DataSource dataSource = DataSourceBuilder.create()
                .url("jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Create base invitations table as defined in V1
            stmt.execute("""
                CREATE TABLE invitations (
                    id UUID DEFAULT random_uuid() PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    role VARCHAR(30) NOT NULL DEFAULT 'EMPLOYEE',
                    department_id UUID,
                    team_id UUID,
                    manager_id UUID,
                    token VARCHAR(255) NOT NULL UNIQUE,
                    invited_by UUID NOT NULL,
                    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
                );
            """);

            // 2. Insert existing historical records (one pending, one accepted)
            stmt.execute("""
                INSERT INTO invitations (id, tenant_id, email, token, invited_by, expires_at, status)
                VALUES ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'pending@test.com', 'token-pending', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW() + INTERVAL '7' DAY, 'PENDING');
            """);
            stmt.execute("""
                INSERT INTO invitations (id, tenant_id, email, token, invited_by, expires_at, status)
                VALUES ('22222222-2222-2222-2222-222222222222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'accepted@test.com', 'token-accepted', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW() + INTERVAL '7' DAY, 'ACCEPTED');
            """);

            // 3. Execute V4 migration script using Spring ScriptUtils
            try (InputStream is = getClass().getResourceAsStream("/db/migration/V4__invitations_email_status.sql")) {
                assertNotNull(is);
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(
                        conn, new org.springframework.core.io.ByteArrayResource(is.readAllBytes())
                );
            }

            // 4. Verify columns matching Invitation entity exist
            var metaData = conn.getMetaData();
            Set<String> columnNames = new HashSet<>();
            try (ResultSet rs = metaData.getColumns(null, null, null, null)) {
                while (rs.next()) {
                    if ("invitations".equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                        columnNames.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }

            assertTrue(columnNames.contains("email_status"), "Must have email_status");
            assertTrue(columnNames.contains("name"), "Must have name");
            assertTrue(columnNames.contains("sent_at"), "Must have sent_at");
            assertTrue(columnNames.contains("error_message"), "Must have error_message");

            // 5. Verify existing pending row has email_status = 'EMAIL_PENDING'
            try (ResultSet rs = stmt.executeQuery("SELECT email_status FROM invitations WHERE id = '11111111-1111-1111-1111-111111111111'")) {
                assertTrue(rs.next());
                assertEquals("EMAIL_PENDING", rs.getString("email_status"));
            }

            // 6. Verify existing accepted row was backfilled to email_status = 'EMAIL_SENT'
            try (ResultSet rs = stmt.executeQuery("SELECT email_status FROM invitations WHERE id = '22222222-2222-2222-2222-222222222222'")) {
                assertTrue(rs.next());
                assertEquals("EMAIL_SENT", rs.getString("email_status"));
            }

            // 7. Verify new insert without email_status gets default 'EMAIL_PENDING'
            stmt.execute("""
                INSERT INTO invitations (id, tenant_id, email, token, invited_by, expires_at)
                VALUES ('33333333-3333-3333-3333-333333333333', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'new@test.com', 'token-new', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', NOW() + INTERVAL '7' DAY);
            """);
            try (ResultSet rs = stmt.executeQuery("SELECT email_status FROM invitations WHERE id = '33333333-3333-3333-3333-333333333333'")) {
                assertTrue(rs.next());
                assertEquals("EMAIL_PENDING", rs.getString("email_status"));
            }
        }
    }
}
