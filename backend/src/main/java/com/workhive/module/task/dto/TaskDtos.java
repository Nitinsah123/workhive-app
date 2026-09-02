package com.workhive.module.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class TaskDtos {

    @Data
    public static class CreateTaskRequest {
        private UUID projectId;

        @NotBlank(message = "Task title is required")
        private String title;

        private String description;
        private UUID assigneeId;
        private UUID reviewerId;
        private String priority = "MEDIUM";
        private String status = "TODO";
        private LocalDate dueDate;
        private String labels;
        private BigDecimal estimatedHours;
        private UUID milestoneId;
        private List<String> subtasks;
    }

    @Data
    public static class UpdateTaskRequest {
        @NotBlank(message = "Task title is required")
        private String title;

        private String description;
        private UUID assigneeId;
        private UUID reviewerId;
        private String priority;
        private String status;
        private LocalDate dueDate;
        private String labels;
        private BigDecimal estimatedHours;
        private BigDecimal actualHours;
        private UUID milestoneId;
        private Integer version;
    }

    @Data
    public static class UpdateTaskStatusRequest {
        @NotBlank(message = "Status is required")
        private String status;
        private String comment;
        private Integer version;
    }

    @Data
    public static class ReviewTaskRequest {
        @NotBlank(message = "Decision is required")
        private String decision; // APPROVED or CHANGES_REQUESTED
        private String comment;
    }

    @Data
    public static class CreateSubtaskRequest {
        @NotBlank(message = "Subtask title is required")
        private String title;
    }

    @Data
    public static class AddCommentRequest {
        @NotBlank(message = "Comment content is required")
        private String content;
    }

    @Data
    public static class SubmitTaskReviewRequest {
        @NotBlank(message = "Work summary is required")
        private String workSummary;

        @NotBlank(message = "Repository URL is required")
        private String repositoryUrl;

        private String provider;
        private String branch;
        private String pullRequestUrl;
        private String commitSha;
    }
}
