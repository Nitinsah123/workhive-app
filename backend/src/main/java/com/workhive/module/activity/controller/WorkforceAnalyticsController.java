package com.workhive.module.activity.controller;

import com.workhive.module.activity.dto.Employee360DashboardDTO;
import com.workhive.module.activity.dto.UnitRosterResponseDTO;
import com.workhive.module.activity.service.ActivityStreamService;
import com.workhive.module.activity.service.WorkforceLiveAnalyticsService;
import com.workhive.security.TenantContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
public class WorkforceAnalyticsController {

    private final WorkforceLiveAnalyticsService analyticsService;
    private final ActivityStreamService activityStreamService;

    public WorkforceAnalyticsController(WorkforceLiveAnalyticsService analyticsService,
                                        ActivityStreamService activityStreamService) {
        this.analyticsService = analyticsService;
        this.activityStreamService = activityStreamService;
    }

    /**
     * Real-time live roster for a Department (aggregated across its teams).
     */
    @GetMapping({"/api/v1/departments/{departmentId}/members/live", "/api/departments/{departmentId}/members/live"})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<UnitRosterResponseDTO> getDepartmentLiveRoster(@PathVariable UUID departmentId) {
        return ResponseEntity.ok(analyticsService.getDepartmentLiveRoster(departmentId));
    }

    /**
     * Real-time live roster for a Team.
     */
    @GetMapping({"/api/v1/teams/{teamId}/members/live", "/api/teams/{teamId}/members/live"})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<UnitRosterResponseDTO> getTeamLiveRoster(@PathVariable UUID teamId) {
        return ResponseEntity.ok(analyticsService.getTeamLiveRoster(teamId));
    }

    /**
     * 360° Employee Activity & Performance Dashboard (Power BI style metrics & telemetry).
     */
    @GetMapping({"/api/v1/analytics/employees/{userId}/360", "/api/analytics/employees/{userId}/360", "/api/employees/{userId}/360", "/api/v1/employees/{userId}/360"})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Employee360DashboardDTO> getEmployee360Dashboard(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "TODAY") String range) {
        return ResponseEntity.ok(analyticsService.getEmployee360Dashboard(userId, range));
    }

    /**
     * Server-Sent Events (SSE) stream for live telemetry, Git push events, and presence heartbeats.
     */
    @GetMapping(value = {"/api/v1/stream/activity", "/api/stream/activity"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamActivities() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return activityStreamService.subscribe(tenantId, userId);
    }
}
