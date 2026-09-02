package com.workhive.module.task.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;

    @Column(name = "repository_url", nullable = false, length = 1000)
    private String repositoryUrl;

    @Column(name = "provider", length = 50)
    @Builder.Default
    private String provider = "GITHUB";

    @Column(name = "external_repository_id", length = 200)
    private String externalRepositoryId;

    @Column(name = "branch", length = 200)
    private String branch;

    @Column(name = "pull_request_url", length = 1000)
    private String pullRequestUrl;

    @Column(name = "commit_sha", length = 100)
    private String commitSha;

    @Column(name = "work_summary", length = 3000)
    private String workSummary;

    @Column(name = "review_status", nullable = false, length = 50)
    @Builder.Default
    private String reviewStatus = "PENDING"; // PENDING, APPROVED, CHANGES_REQUESTED

    @Column(name = "review_comment", length = 3000)
    private String reviewComment;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "version")
    @Builder.Default
    private Integer version = 1;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant submittedAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
