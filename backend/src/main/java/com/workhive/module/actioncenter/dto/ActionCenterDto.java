package com.workhive.module.actioncenter.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public class ActionCenterDto {

    @Data
    @Builder
    public static class ActionItem {
        private UUID id;
        private String type; // LEAVE_REQUEST, TASK_REVIEW, DOCUMENT_APPROVAL
        private String title;
        private String description;
        private String status; // PENDING, APPROVED, REJECTED, CHANGES_REQUESTED
        private Instant createdAt;

        // Requester details
        private UUID requesterId;
        private String requesterName;
        private String requesterEmail;
        private String requesterEmployeeCode;
        private String requesterAvatarUrl;
        private String requesterDepartment;
        private String requesterTeam;

        // Related entity
        private String entityType;
        private UUID entityId;
        private Object metadata;
    }

    @Data
    @Builder
    public static class ActionCenterSummary {
        private long totalPending;
        private long pendingLeaves;
        private long pendingTaskReviews;
        private long pendingDocuments;
    }
}
