package com.workhive.security;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.archive.dto.ArchiveDtos.ArchiveSummaryResponse;
import com.workhive.module.archive.service.ArchiveService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.department.entity.Department;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.department.service.DepartmentService;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.repository.ProjectMemberRepository;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.project.service.ProjectService;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.task.service.TaskService;
import com.workhive.module.team.entity.Team;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.module.team.service.TeamService;
import com.workhive.module.tenant.repository.TenantRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.EmailConnectionRepository;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.module.user.service.UserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArchiveAndPermanentDeleteSecurityTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private EmailConnectionRepository emailConnectionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private EntityManager entityManager;
    @Mock private Query query;

    private UserService userService;
    private DepartmentService departmentService;
    private TeamService teamService;
    private ProjectService projectService;
    private TaskService taskService;
    private ArchiveService archiveService;

    private final UUID TENANT_A = UUID.randomUUID();
    private final UUID TENANT_B = UUID.randomUUID();
    private final UUID ADMIN_A = UUID.randomUUID();
    private final UUID EMPLOYEE_A = UUID.randomUUID();
    private final UUID MANAGER_A = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        userService = new UserService(userRepository, tenantRepository, departmentRepository, teamRepository,
                emailConnectionRepository, null, passwordEncoder, auditService);
        ReflectionTestUtils.setField(userService, "entityManager", entityManager);

        departmentService = new DepartmentService(departmentRepository, auditService);
        ReflectionTestUtils.setField(departmentService, "entityManager", entityManager);

        teamService = new TeamService(teamRepository, auditService);
        ReflectionTestUtils.setField(teamService, "entityManager", entityManager);

        projectService = new ProjectService(projectRepository, projectMemberRepository, null, taskRepository, null, auditService);
        ReflectionTestUtils.setField(projectService, "entityManager", entityManager);

        taskService = new TaskService(taskRepository, null, null, null, null, projectRepository, projectService, userRepository, null, null, auditService);
        ReflectionTestUtils.setField(taskService, "entityManager", entityManager);

        archiveService = new ArchiveService(userRepository, departmentRepository, teamRepository,
                projectRepository, taskRepository, userService, departmentService, teamService, projectService, taskService, auditService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ==========================================
    // USER ARCHIVE & DELETE TESTS
    // ==========================================

    @Test
    void testAdminCanArchiveUser() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        User targetUser = User.builder().id(EMPLOYEE_A).tenantId(TENANT_A).role("EMPLOYEE").status("ACTIVE").build();
        when(userRepository.findByIdAndTenantId(EMPLOYEE_A, TENANT_A)).thenReturn(Optional.of(targetUser));

        userService.archiveUser(EMPLOYEE_A);

        assertEquals("ARCHIVED", targetUser.getStatus());
        verify(userRepository).save(targetUser);
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("USER_ARCHIVED"), eq("USER"), eq(EMPLOYEE_A), any(), any());
    }

    @Test
    void testAdminCanUnarchiveUser() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        User targetUser = User.builder().id(EMPLOYEE_A).tenantId(TENANT_A).role("EMPLOYEE").status("ARCHIVED").build();
        when(userRepository.findByIdAndTenantId(EMPLOYEE_A, TENANT_A)).thenReturn(Optional.of(targetUser));

        userService.unarchiveUser(EMPLOYEE_A);

        assertEquals("ACTIVE", targetUser.getStatus());
        verify(userRepository).save(targetUser);
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("USER_RESTORED"), eq("USER"), eq(EMPLOYEE_A), any(), any());
    }

    @Test
    void testAdminCannotDeleteSelf() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.permanentDeleteUser(ADMIN_A));
        assertTrue(ex.getMessage().contains("cannot delete your own account"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testAdminCannotDeleteLastTenantAdmin() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        UUID otherAdminId = UUID.randomUUID();
        User otherAdmin = User.builder().id(otherAdminId).tenantId(TENANT_A).role("TENANT_ADMIN").status("ACTIVE").build();
        when(userRepository.findByIdAndTenantId(otherAdminId, TENANT_A)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countByTenantIdAndRole(TENANT_A, "TENANT_ADMIN")).thenReturn(1L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.permanentDeleteUser(otherAdminId));
        assertTrue(ex.getMessage().contains("Cannot delete the only Tenant Admin"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testUnauthorizedRolesCannotDeleteUser() {
        // Employee attempting deletion
        TenantContext.setContext(EMPLOYEE_A, TENANT_A, "EMPLOYEE");
        assertThrows(BadRequestException.class, () -> userService.permanentDeleteUser(UUID.randomUUID()));

        // Manager attempting deletion
        TenantContext.setContext(MANAGER_A, TENANT_A, "MANAGER");
        assertThrows(BadRequestException.class, () -> userService.permanentDeleteUser(UUID.randomUUID()));
    }

    @Test
    void testPermanentDeleteCleansDependenciesAndDeletesUser() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        User targetUser = User.builder().id(EMPLOYEE_A).tenantId(TENANT_A).role("EMPLOYEE").status("ACTIVE").build();
        when(userRepository.findByIdAndTenantId(EMPLOYEE_A, TENANT_A)).thenReturn(Optional.of(targetUser));

        userService.permanentDeleteUser(EMPLOYEE_A);

        verify(entityManager, atLeastOnce()).createNativeQuery(anyString());
        verify(userRepository).delete(targetUser);
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("USER_PERMANENTLY_DELETED"), eq("USER"), eq(EMPLOYEE_A), any(), any());
    }

    // ==========================================
    // DEPARTMENT ARCHIVE & DELETE TESTS
    // ==========================================

    @Test
    void testDepartmentArchiveAndRestore() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        UUID deptId = UUID.randomUUID();
        Department dept = Department.builder().id(deptId).tenantId(TENANT_A).name("Engineering").status("ACTIVE").build();
        when(departmentRepository.findByIdAndTenantId(deptId, TENANT_A)).thenReturn(Optional.of(dept));

        departmentService.archiveDepartment(deptId);
        assertEquals("ARCHIVED", dept.getStatus());
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("DEPARTMENT_ARCHIVED"), eq("DEPARTMENT"), eq(deptId), any(), any());

        departmentService.unarchiveDepartment(deptId);
        assertEquals("ACTIVE", dept.getStatus());
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("DEPARTMENT_RESTORED"), eq("DEPARTMENT"), eq(deptId), any(), any());
    }

    @Test
    void testDepartmentPermanentDeleteSafelyDetaches() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        UUID deptId = UUID.randomUUID();
        Department dept = Department.builder().id(deptId).tenantId(TENANT_A).name("Sales").status("ARCHIVED").build();
        when(departmentRepository.findByIdAndTenantId(deptId, TENANT_A)).thenReturn(Optional.of(dept));

        departmentService.permanentDeleteDepartment(deptId);

        verify(entityManager, atLeastOnce()).createNativeQuery(anyString());
        verify(departmentRepository).delete(dept);
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("DEPARTMENT_PERMANENTLY_DELETED"), eq("DEPARTMENT"), eq(deptId), any(), any());
    }

    // ==========================================
    // TEAM ARCHIVE & DELETE TESTS
    // ==========================================

    @Test
    void testTeamArchiveAndRestore() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        UUID teamId = UUID.randomUUID();
        Team team = Team.builder().id(teamId).tenantId(TENANT_A).name("Alpha").status("ACTIVE").build();
        when(teamRepository.findByIdAndTenantId(teamId, TENANT_A)).thenReturn(Optional.of(team));

        teamService.archiveTeam(teamId);
        assertEquals("ARCHIVED", team.getStatus());
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("TEAM_ARCHIVED"), eq("TEAM"), eq(teamId), any(), any());

        teamService.unarchiveTeam(teamId);
        assertEquals("ACTIVE", team.getStatus());
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("TEAM_RESTORED"), eq("TEAM"), eq(teamId), any(), any());
    }

    @Test
    void testTeamPermanentDeleteSafelyDetaches() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        UUID teamId = UUID.randomUUID();
        Team team = Team.builder().id(teamId).tenantId(TENANT_A).name("Beta").status("ARCHIVED").build();
        when(teamRepository.findByIdAndTenantId(teamId, TENANT_A)).thenReturn(Optional.of(team));

        teamService.permanentDeleteTeam(teamId);

        verify(entityManager, atLeastOnce()).createNativeQuery(anyString());
        verify(teamRepository).delete(team);
        verify(auditService).log(eq(TENANT_A), eq(ADMIN_A), eq("TEAM_PERMANENTLY_DELETED"), eq("TEAM"), eq(teamId), any(), any());
    }

    // ==========================================
    // DEDICATED ARCHIVE SECTION TESTS
    // ==========================================

    @Test
    void testArchiveSummaryDistinguishesTypesAndPresentsRealRecords() {
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        User u = User.builder().id(UUID.randomUUID()).tenantId(TENANT_A).fullName("Archived Person").email("p@test.com").role("EMPLOYEE").status("ARCHIVED").build();
        Department d = Department.builder().id(UUID.randomUUID()).tenantId(TENANT_A).name("Old Dept").status("ARCHIVED").build();
        Team tm = Team.builder().id(UUID.randomUUID()).tenantId(TENANT_A).name("Old Team").status("ARCHIVED").build();
        Project p = Project.builder().id(UUID.randomUUID()).tenantId(TENANT_A).name("Old Project").status("ARCHIVED").build();
        Task tk = Task.builder().id(UUID.randomUUID()).tenantId(TENANT_A).title("Old Task").status("ARCHIVED").build();

        when(userRepository.findByTenantIdAndStatus(TENANT_A, "ARCHIVED")).thenReturn(List.of(u));
        when(departmentRepository.findByTenantIdAndStatus(TENANT_A, "ARCHIVED")).thenReturn(List.of(d));
        when(teamRepository.findByTenantIdAndStatus(TENANT_A, "ARCHIVED")).thenReturn(List.of(tm));
        when(projectRepository.findByTenantIdAndStatus(TENANT_A, "ARCHIVED")).thenReturn(List.of(p));
        when(taskRepository.findByTenantIdAndStatus(TENANT_A, "ARCHIVED")).thenReturn(List.of(tk));

        ArchiveSummaryResponse summary = archiveService.getArchiveSummary("ALL");

        assertEquals(5, summary.getTotalCount());
        assertEquals(1, summary.getUsersCount());
        assertEquals(1, summary.getDepartmentsCount());
        assertEquals(1, summary.getTeamsCount());
        assertEquals(1, summary.getProjectsCount());
        assertEquals(1, summary.getTasksCount());
        assertEquals(5, summary.getItems().size());
    }

    // ==========================================
    // TENANT ISOLATION TESTS
    // ==========================================

    @Test
    void testTenantCannotAccessOrDeleteOtherTenantRecords() {
        // Tenant A tries to access Tenant B's department
        TenantContext.setContext(ADMIN_A, TENANT_A, "TENANT_ADMIN");

        UUID deptTenantB = UUID.randomUUID();
        when(departmentRepository.findByIdAndTenantId(deptTenantB, TENANT_A)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartment(deptTenantB));
        assertThrows(ResourceNotFoundException.class, () -> departmentService.permanentDeleteDepartment(deptTenantB));

        // Tenant A tries to access Tenant B's user
        UUID userTenantB = UUID.randomUUID();
        when(userRepository.findByIdAndTenantId(userTenantB, TENANT_A)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.permanentDeleteUser(userTenantB));
    }
}
