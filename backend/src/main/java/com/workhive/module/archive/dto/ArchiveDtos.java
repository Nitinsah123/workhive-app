package com.workhive.module.archive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ArchiveDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchiveSummaryResponse {
        private List<ArchivedItemDto> items;
        private long totalCount;
        private long usersCount;
        private long departmentsCount;
        private long teamsCount;
        private long projectsCount;
        private long tasksCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArchivedItemDto {
        private String id;
        private String type; // USER, DEPARTMENT, TEAM, PROJECT, TASK
        private String title;
        private String description;
        private String status;
        private Instant archivedAt;
        private String archivedBy;
        private Map<String, Object> metadata;
    }
}
