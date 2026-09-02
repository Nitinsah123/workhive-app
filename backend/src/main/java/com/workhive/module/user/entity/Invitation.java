package com.workhive.module.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invitation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String email;

    @Column(name = "name")
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private String role = "EMPLOYEE";

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(nullable = false, unique = true, length = 1000)
    private String token;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACCEPTED, REVOKED, EXPIRED

    @Column(name = "email_status", nullable = false)
    @Builder.Default
    private String emailStatus = "EMAIL_PENDING"; // EMAIL_PENDING, EMAIL_SENT, EMAIL_FAILED

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
