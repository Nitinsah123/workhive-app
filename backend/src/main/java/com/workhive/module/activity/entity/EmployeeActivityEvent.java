package com.workhive.module.activity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee_activity_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(length = 500)
    private String title;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
