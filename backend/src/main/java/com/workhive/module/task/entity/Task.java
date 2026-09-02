package com.workhive.module.task.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(nullable = false, length = 500)
    private String title;

    private String description;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    @Column(nullable = false) @Builder.Default
    private String priority = "MEDIUM";

    @Column(nullable = false) @Builder.Default
    private String status = "TODO";

    @Column(name = "due_date")
    private LocalDate dueDate;

    private String labels;

    @Column(name = "estimated_hours")
    private BigDecimal estimatedHours;

    @Column(name = "actual_hours") @Builder.Default
    private BigDecimal actualHours = BigDecimal.ZERO;

    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "sort_order") @Builder.Default
    private Integer sortOrder = 0;

    @Version
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false) @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate protected void onUpdate() { this.updatedAt = Instant.now(); }
}
