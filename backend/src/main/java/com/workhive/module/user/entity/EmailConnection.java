package com.workhive.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_connections",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_email_conn_tenant_user_provider",
           columnNames = {"tenant_id", "user_id", "provider"}
       ))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String provider = "GMAIL"; // GMAIL, OUTLOOK, etc.

    @Column(name = "email_address", length = 255)
    private String emailAddress; // The authorized sender email

    @Column(name = "access_token_enc", columnDefinition = "TEXT")
    private String accessTokenEnc; // AES-GCM encrypted

    @Column(name = "refresh_token_enc", columnDefinition = "TEXT")
    private String refreshTokenEnc; // AES-GCM encrypted

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(length = 500)
    private String scopes;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "NOT_CONNECTED"; // NOT_CONNECTED, CONNECTED, ERROR, REAUTH_REQUIRED, DISCONNECTED

    @Column(name = "oauth_state", length = 255)
    private String oauthState; // CSRF state for OAuth flow

    @Column(name = "last_send_at")
    private Instant lastSendAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
