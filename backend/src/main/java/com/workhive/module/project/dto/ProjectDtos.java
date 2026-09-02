package com.workhive.module.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ProjectDtos {

    @Data
    public static class CreateProjectRequest {
        @NotBlank(message = "Project name is required")
        private String name;
        private String description;
        private UUID managerId;
        private UUID departmentId;
        private UUID teamId;
        private String priority = "MEDIUM";
        private String status = "PLANNING";
        private LocalDate startDate;
        private LocalDate targetDate;
        private List<UUID> memberIds;
    }

    @Data
    public static class UpdateProjectRequest {
        @NotBlank(message = "Project name is required")
        private String name;
        private String description;
        private UUID managerId;
        private UUID departmentId;
        private UUID teamId;
        private String priority;
        private String status;
        private LocalDate startDate;
        private LocalDate targetDate;
    }

    @Data
    public static class CreateMilestoneRequest {
        @NotBlank(message = "Milestone name is required")
        private String name;
        private String description;
        private LocalDate targetDate;
    }

    @Data
    public static class ProjectDetailResponse {
        private Object project;
        private List<Object> members;
        private List<Object> milestones;
        private long totalTasks;
        private long completedTasks;
        private long inProgressTasks;
        private long reviewTasks;
        private long overdueTasks;
    }
}
