package com.workhive.security;

import org.springframework.security.access.AccessDeniedException;
import com.workhive.module.activity.entity.WorkActivity;
import com.workhive.module.activity.repository.WorkActivityRepository;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.activity.service.WorkActivityService.ActivityHeatmapResponse;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.module.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkActivitySecurityAndScopingTest {

    @Mock private WorkActivityRepository workActivityRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private WorkActivityService workActivityService;
    private UserService userService;

    private final UUID TENANT_ID = UUID.randomUUID();
    private final UUID ADMIN_ID = UUID.randomUUID();
    private final UUID MANAGER_ID = UUID.randomUUID();
    private final UUID EMPLOYEE_1_ID = UUID.randomUUID();
    private final UUID EMPLOYEE_2_ID = UUID.randomUUID();

    private User adminUser;
    private User managerUser;
    private User employee1;
    private User employee2;

    @BeforeEach
    void setUp() {
        workActivityService = new WorkActivityService(workActivityRepository, userRepository, messagingTemplate);
        userService = new UserService(
                userRepository, null, null, null, null, null, null, null
        );

        adminUser = User.builder().id(ADMIN_ID).tenantId(TENANT_ID).email("admin@workhive.com").role("TENANT_ADMIN").status("ACTIVE").build();
        managerUser = User.builder().id(MANAGER_ID).tenantId(TENANT_ID).email("manager@workhive.com").role("MANAGER").status("ACTIVE").build();
        employee1 = User.builder().id(EMPLOYEE_1_ID).tenantId(TENANT_ID).email("emp1@workhive.com").role("EMPLOYEE").managerId(MANAGER_ID).status("ACTIVE").build();
        employee2 = User.builder().id(EMPLOYEE_2_ID).tenantId(TENANT_ID).email("emp2@workhive.com").role("EMPLOYEE").status("ACTIVE").build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Scenario 9 & 10: Admin sees all active workspace members and can query aggregated activities")
    void testAdminViewsAllWorkspaceMembersAndAggregatedActivity() {
        TenantContext.setContext(ADMIN_ID, TENANT_ID, "TENANT_ADMIN");

        when(userRepository.findByTenantIdAndStatus(TENANT_ID, "ACTIVE"))
                .thenReturn(List.of(adminUser, managerUser, employee1, employee2));

        List<User> activeMembers = userService.getActiveUsers();
        assertEquals(4, activeMembers.size(), "Admin must see all active tenant users across roles");

        Page<WorkActivity> mockPage = new PageImpl<>(List.of(
                WorkActivity.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).userId(EMPLOYEE_1_ID).activityType("TASK").build(),
                WorkActivity.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).userId(MANAGER_ID).activityType("PROJECT").build()
        ));
        when(workActivityRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), any()))
                .thenReturn(mockPage);

        Page<WorkActivity> result = workActivityService.getActivities(null, PageRequest.of(0, 20));
        assertEquals(2, result.getContent().size());
        verify(workActivityRepository).findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID), any());
    }

    @Test
    @DisplayName("Scenario 11: Admin can select an individual member and query only that member's activity")
    void testAdminQueriesIndividualMemberActivity() {
        TenantContext.setContext(ADMIN_ID, TENANT_ID, "TENANT_ADMIN");

        when(userRepository.findByIdAndTenantId(EMPLOYEE_1_ID, TENANT_ID))
                .thenReturn(Optional.of(employee1));

        Page<WorkActivity> mockPage = new PageImpl<>(List.of(
                WorkActivity.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).userId(EMPLOYEE_1_ID).activityType("TASK").build()
        ));
        when(workActivityRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(eq(TENANT_ID), eq(EMPLOYEE_1_ID), any()))
                .thenReturn(mockPage);

        Page<WorkActivity> result = workActivityService.getActivities(EMPLOYEE_1_ID, PageRequest.of(0, 20));
        assertEquals(1, result.getContent().size());
        assertEquals(EMPLOYEE_1_ID, result.getContent().get(0).getUserId());
    }

    @Test
    @DisplayName("Scenario 12: Manager sees only authorized scope (managed reports + self)")
    void testManagerScopeEnforcement() {
        TenantContext.setContext(MANAGER_ID, TENANT_ID, "MANAGER");

        when(userRepository.findByTenantIdAndManagerIdAndStatus(TENANT_ID, MANAGER_ID, "ACTIVE"))
                .thenReturn(List.of(employee1));
        when(userRepository.findByIdAndTenantId(MANAGER_ID, TENANT_ID))
                .thenReturn(Optional.of(managerUser));

        List<User> managerMembers = userService.getActiveUsers();
        assertEquals(2, managerMembers.size(), "Manager should see self + direct reports");
        assertTrue(managerMembers.contains(managerUser));
        assertTrue(managerMembers.contains(employee1));

        // Manager queries authorized report
        when(workActivityRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(eq(TENANT_ID), eq(EMPLOYEE_1_ID), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        assertDoesNotThrow(() -> workActivityService.getActivities(EMPLOYEE_1_ID, PageRequest.of(0, 20)));

        // Manager attempts to query unauthorized user (employee2)
        assertThrows(AccessDeniedException.class, () -> {
            workActivityService.getActivities(EMPLOYEE_2_ID, PageRequest.of(0, 20));
        });
    }

    @Test
    @DisplayName("Scenario 13: Employee sees only own activity and cannot manipulate userId param")
    void testEmployeeSelfOnlyEnforcement() {
        TenantContext.setContext(EMPLOYEE_1_ID, TENANT_ID, "EMPLOYEE");

        when(userRepository.findByIdAndTenantId(EMPLOYEE_1_ID, TENANT_ID))
                .thenReturn(Optional.of(employee1));

        List<User> employeeActive = userService.getActiveUsers();
        assertEquals(1, employeeActive.size());
        assertEquals(EMPLOYEE_1_ID, employeeActive.get(0).getId());

        // Employee queries own activity without param
        when(workActivityRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(eq(TENANT_ID), eq(EMPLOYEE_1_ID), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        assertDoesNotThrow(() -> workActivityService.getActivities(null, PageRequest.of(0, 20)));

        // Employee attempts to manipulate userId param to view employee2's activity -> rejected!
        assertThrows(AccessDeniedException.class, () -> {
            workActivityService.getActivities(EMPLOYEE_2_ID, PageRequest.of(0, 20));
        });

        // Employee attempts to manipulate heatmap userId param -> rejected!
        assertThrows(AccessDeniedException.class, () -> {
            workActivityService.getHeatmap(EMPLOYEE_2_ID);
        });
    }

    @Test
    @DisplayName("Scenario 14: Real-time updates broadcast to scoped topics")
    void testRealTimeScopedBroadcast() {
        WorkActivity act = WorkActivity.builder()
                .tenantId(TENANT_ID)
                .userId(EMPLOYEE_1_ID)
                .activityType("COMMIT")
                .title("GitHub Commit: Add security tests")
                .build();
        when(workActivityRepository.save(any(WorkActivity.class))).thenReturn(act);

        workActivityService.recordActivity(TENANT_ID, EMPLOYEE_1_ID, null, null,
                "GITHUB", "COMMIT", "GitHub Commit: Add security tests", "Description", "evt-123", "http://github.com");

        // Verify broadcast to user's private topic
        verify(messagingTemplate).convertAndSend(eq("/topic/tenant." + TENANT_ID + ".user." + EMPLOYEE_1_ID + ".activities"), eq(act));
        // Verify broadcast to tenant activities topic
        verify(messagingTemplate).convertAndSend(eq("/topic/tenant." + TENANT_ID + ".activities"), eq(act));
    }

    @Test
    @DisplayName("Scenario 15: 12-month contribution map & velocity calculate from real stored data")
    void testHeatmapCalculatesFromRealStoredData() {
        TenantContext.setContext(EMPLOYEE_1_ID, TENANT_ID, "EMPLOYEE");

        Instant now = Instant.now();
        List<WorkActivity> realActivities = List.of(
                WorkActivity.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).userId(EMPLOYEE_1_ID).activityType("COMMIT").createdAt(now).build(),
                WorkActivity.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).userId(EMPLOYEE_1_ID).activityType("PULL_REQUEST").createdAt(now).build()
        );

        when(workActivityRepository.findByTenantIdAndUserIdAndCreatedAtBetween(eq(TENANT_ID), eq(EMPLOYEE_1_ID), any(), any()))
                .thenReturn(realActivities);

        ActivityHeatmapResponse heatmap = workActivityService.getHeatmap(null);
        assertNotNull(heatmap);
        assertEquals(2, heatmap.getTotalActivities(), "Total activities must match real count");
        assertEquals(1, heatmap.getActiveDays(), "Active days must match real days with events");
        assertEquals(2, heatmap.getMaxDayCount(), "Peak daily velocity must match maximum daily count");
        assertEquals(365, heatmap.getDays().size(), "Heatmap must include 365 daily slots");
    }
}
