package com.workhive.module.activity.service;

import com.workhive.module.activity.entity.WorkActivity;
import com.workhive.module.activity.repository.WorkActivityRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkActivityService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkActivityService.class);

    private final WorkActivityRepository workActivityRepository;
    private final com.workhive.module.user.repository.UserRepository userRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public WorkActivityService(WorkActivityRepository workActivityRepository,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) com.workhive.module.user.repository.UserRepository userRepository,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.workActivityRepository = workActivityRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public WorkActivityService(WorkActivityRepository workActivityRepository) {
        this(workActivityRepository, null, null);
    }

    @Transactional
    public WorkActivity recordActivity(UUID tenantId, UUID userId, UUID projectId, UUID taskId,
                                       String source, String activityType, String title,
                                       String description, String externalEventId, String externalUrl) {
        WorkActivity activity = WorkActivity.builder()
                .tenantId(tenantId)
                .userId(userId)
                .projectId(projectId)
                .taskId(taskId)
                .source(source != null ? source : "WORKHIVE")
                .activityType(activityType)
                .title(title)
                .description(description)
                .externalEventId(externalEventId)
                .externalUrl(externalUrl)
                .build();
        WorkActivity saved = workActivityRepository.save(activity);

        // Real-time scoped WebSocket broadcasts
        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/tenant." + tenantId + ".user." + userId + ".activities", saved);
                messagingTemplate.convertAndSend("/topic/tenant." + tenantId + ".activities", saved);
            } catch (Exception e) {
                log.debug("WebSocket activity dispatch note: {}", e.getMessage());
            }
        }
        return saved;
    }

    /**
     * Resolves the set of authorized user IDs for activity and heatmap queries.
     * Returns null if all tenant members are authorized (e.g. TENANT_ADMIN with no member filter).
     */
    public java.util.Set<UUID> resolveAuthorizedUserScope(UUID requestedUserId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID callerId = TenantContext.requireUserId();
        String role = TenantContext.getRole();

        if ("TENANT_ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role)) {
            if (requestedUserId == null) {
                return null; // All tenant users
            }
            if (userRepository != null && !userRepository.findByIdAndTenantId(requestedUserId, tenantId).isPresent()) {
                throw new com.workhive.common.exception.ResourceNotFoundException("User", "id", requestedUserId);
            }
            return java.util.Collections.singleton(requestedUserId);
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            java.util.Set<UUID> authorized = new java.util.HashSet<>();
            authorized.add(callerId);
            if (userRepository != null) {
                userRepository.findByTenantIdAndManagerIdAndStatus(tenantId, callerId, "ACTIVE")
                        .forEach(u -> authorized.add(u.getId()));
            }

            if (requestedUserId != null) {
                if (!authorized.contains(requestedUserId)) {
                    throw new org.springframework.security.access.AccessDeniedException("Unauthorized to view activities for user: " + requestedUserId);
                }
                return java.util.Collections.singleton(requestedUserId);
            }
            return authorized;
        } else {
            // EMPLOYEE: strictly self-only
            if (requestedUserId != null && !callerId.equals(requestedUserId)) {
                throw new org.springframework.security.access.AccessDeniedException("Employees may only view their own activity");
            }
            return java.util.Collections.singleton(callerId);
        }
    }

    public Page<WorkActivity> getActivities(UUID requestedUserId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        java.util.Set<UUID> userScope = resolveAuthorizedUserScope(requestedUserId);

        if (userScope == null) {
            return workActivityRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        } else if (userScope.size() == 1) {
            return workActivityRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userScope.iterator().next(), pageable);
        } else {
            return workActivityRepository.findByTenantIdAndUserIdInOrderByCreatedAtDesc(tenantId, userScope, pageable);
        }
    }

    public Page<WorkActivity> getTenantActivities(Pageable pageable) {
        return getActivities(null, pageable);
    }

    public Page<WorkActivity> getMyActivities(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return workActivityRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId, pageable);
    }

    public Page<WorkActivity> getProjectActivities(UUID projectId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return workActivityRepository.findByTenantIdAndProjectIdOrderByCreatedAtDesc(tenantId, projectId, pageable);
    }

    public ActivityHeatmapResponse getHeatmap(UUID targetUserId) {
        UUID tenantId = TenantContext.requireTenantId();
        java.util.Set<UUID> userScope = resolveAuthorizedUserScope(targetUserId);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate oneYearAgo = today.minusDays(364);
        java.time.Instant start = oneYearAgo.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant end = today.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        java.util.List<WorkActivity> activities;
        if (userScope == null) {
            activities = workActivityRepository.findByTenantIdAndCreatedAtBetween(tenantId, start, end);
        } else if (userScope.size() == 1) {
            activities = workActivityRepository.findByTenantIdAndUserIdAndCreatedAtBetween(tenantId, userScope.iterator().next(), start, end);
        } else {
            activities = workActivityRepository.findByTenantIdAndUserIdInAndCreatedAtBetween(tenantId, userScope, start, end);
        }

        // Group activities by date string "YYYY-MM-DD"
        java.util.Map<String, java.util.List<WorkActivity>> byDate = new java.util.HashMap<>();
        for (WorkActivity act : activities) {
            String dateStr = act.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate().toString();
            byDate.computeIfAbsent(dateStr, k -> new java.util.ArrayList<>()).add(act);
        }

        int totalActivities = activities.size();
        int activeDays = byDate.size();
        int maxCount = 0;
        for (java.util.List<WorkActivity> list : byDate.values()) {
            if (list.size() > maxCount) maxCount = list.size();
        }

        java.util.List<HeatmapDayDto> days = new java.util.ArrayList<>();
        java.time.LocalDate cursor = oneYearAgo;
        while (!cursor.isAfter(today)) {
            String dateStr = cursor.toString();
            java.util.List<WorkActivity> dayActs = byDate.getOrDefault(dateStr, java.util.Collections.emptyList());
            int count = dayActs.size();

            int intensity = 0;
            if (count > 0) {
                if (count >= 8) intensity = 4;
                else if (count >= 5) intensity = 3;
                else if (count >= 2) intensity = 2;
                else intensity = 1;
            }

            java.util.Map<String, Integer> types = new java.util.HashMap<>();
            for (WorkActivity a : dayActs) {
                types.merge(a.getActivityType(), 1, Integer::sum);
            }

            days.add(HeatmapDayDto.builder()
                    .date(dateStr)
                    .count(count)
                    .intensity(intensity)
                    .types(types)
                    .build());

            cursor = cursor.plusDays(1);
        }

        return ActivityHeatmapResponse.builder()
                .totalActivities(totalActivities)
                .activeDays(activeDays)
                .maxDayCount(maxCount)
                .days(days)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HeatmapDayDto {
        private String date;
        private int count;
        private int intensity;
        private java.util.Map<String, Integer> types;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ActivityHeatmapResponse {
        private int totalActivities;
        private int activeDays;
        private int maxDayCount;
        private java.util.List<HeatmapDayDto> days;
    }
}
