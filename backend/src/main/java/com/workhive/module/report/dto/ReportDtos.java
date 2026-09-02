package com.workhive.module.report.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class ReportDtos {

    @Data
    @Builder
    public static class EmployeeWorkReport {
        private String employeeName;
        private String employeeCode;
        private String department;
        private String team;
        private long assignedTasks;
        private long completedTasks;
        private long inProgressTasks;
        private long reviewTasks;
        private long overdueTasks;
        private double completionRate;
        private long totalTimeLoggedMinutes;
        private long daysPresent;
        private long leaveDaysTaken;
        private List<Object> recentActivities;
    }

    @Data
    @Builder
    public static class ProjectReport {
        private String projectName;
        private String status;
        private String health;
        private int progress;
        private long totalTasks;
        private long completedTasks;
        private long todoTasks;
        private long inProgressTasks;
        private long reviewTasks;
        private long overdueTasks;
        private long totalMembers;
        private long totalMilestones;
        private long completedMilestones;
    }

    @Data
    @Builder
    public static class OrganizationReport {
        private String organizationName;
        private String organizationCode;
        private long totalEmployees;
        private long totalDepartments;
        private long totalTeams;
        private long activeProjects;
        private long totalTasks;
        private long completedTasks;
        private long presentToday;
        private long pendingLeaveRequests;
        private long pendingTaskReviews;
        private Map<String, Long> projectHealthDistribution;
        private Map<String, Long> taskStatusDistribution;
    }
}
