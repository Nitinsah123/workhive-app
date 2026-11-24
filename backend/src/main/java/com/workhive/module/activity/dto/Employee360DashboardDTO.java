package com.workhive.module.activity.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee360DashboardDTO {
    private UUID userId;
    private String fullName;
    private String email;
    private String employeeCode;
    private String role;
    private String organizationalRole;
    private String avatarUrl;
    private String departmentName;
    private String teamName;
    private String presenceStatus;
    private String activeFocusTask;
    private String activeGitBranch;
    private String timeRange; // TODAY, 7D, 30D, CURRENT_SPRINT

    // KPI Summary
    private double activeWorkHours;
    private int efficiencyPercentage;
    private long tasksAssigned;
    private long tasksPending;
    private long tasksCompleted;
    private int taskCompletionRatePercentage;
    private long commitsPushed;
    private long prsOpenedOrMerged;
    private long linesChanged;
    private int currentStreakDays;
    private int slaAdherencePercentage;

    // Charts data
    private List<HourlyActivityPointDto> hourlyActivity;
    private List<TaskBreakdownItemDto> taskBreakdown;
    private List<RolePerformanceRadarDto> rolePerformance;
    private List<DailyVelocityPointDto> dailyVelocity;

    // Real-Time Telemetry & Git Push Feed
    private List<ActivityTimelineItemDto> activityTimeline;

    // Assigned / Pending tasks list
    private List<TaskSummaryItemDto> assignedTasks;
    private List<TaskSummaryItemDto> pendingTasks;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HourlyActivityPointDto {
        private String hour;
        private int activeMinutes;
        private int idleMinutes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TaskBreakdownItemDto {
        private String name;
        private long value;
        private String color;
        private String status;
        private String priority;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RolePerformanceRadarDto {
        private String subject;
        private int score;
        private int benchmark;
        private String fullMark;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DailyVelocityPointDto {
        private String date;
        private long commits;
        private long tasksDone;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ActivityTimelineItemDto {
        private UUID id;
        private String eventType; // GIT_PUSH, GIT_PR, TASK_ASSIGNED, TASK_COMPLETED, HEARTBEAT
        private String title;
        private String description;
        private String timestamp;
        private String repository;
        private String branch;
        private String commitSha;
        private String externalUrl;
        private String metadataJson;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TaskSummaryItemDto {
        private UUID id;
        private String title;
        private String status;
        private String priority;
        private String dueDate;
        private boolean overdue;
        private String projectName;
    }
}
