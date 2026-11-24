package com.workhive.module.activity.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitMemberLiveDTO {
    private UUID userId;
    private String fullName;
    private String email;
    private String employeeCode;
    private String avatarUrl;
    private String role;
    private String organizationalRole;
    private UUID departmentId;
    private String departmentName;
    private UUID teamId;
    private String teamName;
    private String presenceStatus; // "Coding / Active", "In Sync", "Idle", "Offline"
    private long assignedTasksCount;
    private long pendingTasksCount;
    private long completedTasksCount;
    private long overdueTasksCount;
    private double activeHoursToday;
    private long gitCommitsToday;
    private long gitPrsToday;
    private List<String> topTaskTitles;
    private String lastActiveTimeFormatted;
}
