package com.workhive.module.announcement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "target_type", nullable = false, length = 30)
    @Builder.Default
    private String targetType = "ORGANIZATION"; // ORGANIZATION, DEPARTMENT, TEAM, PROJECT

    @Column(name = "target_id")
    private UUID targetId;

    @Column(length = 20)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(nullable = false)
    @Builder.Default
    private Boolean published = true;

    @Column(name = "published_at")
    @Builder.Default
    private Instant publishedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

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
