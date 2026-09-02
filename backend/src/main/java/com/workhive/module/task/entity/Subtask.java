package com.workhive.module.task.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "subtasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subtask {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "task_id", nullable = false) private UUID taskId;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(nullable = false, length = 500) private String title;
    @Column(nullable = false) @Builder.Default private Boolean completed = false;
    @Column(name = "sort_order") @Builder.Default private Integer sortOrder = 0;
    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
