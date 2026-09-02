package com.workhive.module.activity.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "work_activities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String source = "WORKHIVE";

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "external_event_id", length = 255)
    private String externalEventId;

    @Column(name = "external_url", length = 500)
    private String externalUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
