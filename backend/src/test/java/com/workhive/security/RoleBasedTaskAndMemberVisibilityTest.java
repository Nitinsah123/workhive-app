package com.workhive.security;

import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.notification.service.NotificationService;
import com.workhive.module.project.dto.ProjectDtos.ProjectMemberDTO;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.entity.ProjectMember;
import com.workhive.module.project.repository.MilestoneRepository;
import com.workhive.module.project.repository.ProjectMemberRepository;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.project.service.ProjectService;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.SubtaskRepository;
import com.workhive.module.task.repository.TaskCommentRepository;
import com.workhive.module.task.repository.TaskHistoryRepository;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.task.repository.TaskSubmissionRepository;
import com.workhive.module.task.service.TaskService;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleBasedTaskAndMemberVisibilityTest {

    @Mock private TaskRepository taskRepository;
    @Mock private SubtaskRepository subtaskRepository;
    @Mock private TaskCommentRepository taskCommentRepository;
    @Mock private TaskHistoryRepository taskHistoryRepository;
    @Mock private TaskSubmissionRepository taskSubmissionRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private WorkActivityService workActivityService;
    @Mock private AuditService auditService;

    private ProjectService projectService;
    private TaskService taskService;

    private final UUID TENANT_ID = UUID.randomUUID();
    private final UUID ADMIN_ID = UUID.randomUUID();
    private final UUID MANAGER_ID = UUID.randomUUID();
    private final UUID EMPLOYEE_ID = UUID.randomUUID();
    private final UUID OTHER_EMP_ID = UUID.randomUUID();
    private final UUID PROJECT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, projectMemberRepository,
                milestoneRepository, taskRepository, workActivityService, auditService);
        ReflectionTestUtils.setField(projectService, "userRepository", userRepository);

        taskService = new TaskService(taskRepository, subtaskRepository, taskCommentRepository,
                taskHistoryRepository, taskSubmissionRepository, projectRepository, projectService,
                userRepository, notificationService, workActivityService, auditService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testEmployeeOnlySeesOwnAssignedTasksAcrossTenantAndProjects() {
        TenantContext.setContext(EMPLOYEE_ID, TENANT_ID, "EMPLOYEE");

        Task empTask = Task.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .projectId(PROJECT_ID)
                .title("My Feature")
                .assigneeId(EMPLOYEE_ID)
                .creatorId(MANAGER_ID)
                .build();

        when(taskRepository.findByTenantIdAndAssigneeId(eq(TENANT_ID), eq(EMPLOYEE_ID), any()))
                .thenReturn(new PageImpl<>(List.of(empTask)));
        when(taskRepository.findByTenantIdAndProjectIdAndAssigneeId(eq(TENANT_ID), eq(PROJECT_ID), eq(EMPLOYEE_ID), any()))
                .thenReturn(new PageImpl<>(List.of(empTask)));

        // Even if an employee attempts to pass another employee's assigneeId, backend ignores it and enforces EMPLOYEE_ID
        var allResult = taskService.getTasks(null, OTHER_EMP_ID, PageRequest.of(0, 20));
        assertEquals(1, allResult.getContent().size());
        verify(taskRepository).findByTenantIdAndAssigneeId(eq(TENANT_ID), eq(EMPLOYEE_ID), any());
        verify(taskRepository, never()).findByTenantId(any(), any());
        verify(taskRepository, never()).findByTenantIdAndAssigneeId(eq(TENANT_ID), eq(OTHER_EMP_ID), any());

        var projectResult = taskService.getTasks(PROJECT_ID, OTHER_EMP_ID, PageRequest.of(0, 20));
        assertEquals(1, projectResult.getContent().size());
        verify(taskRepository).findByTenantIdAndProjectIdAndAssigneeId(eq(TENANT_ID), eq(PROJECT_ID), eq(EMPLOYEE_ID), any());
        verify(taskRepository, never()).findByTenantIdAndProjectId(any(), any(), any());
    }

    @Test
    void testAdminAndManagerSeeAllTasksAndExactNamesAreEnriched() {
        Task t1 = Task.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).projectId(PROJECT_ID)
                .title("Task One").assigneeId(EMPLOYEE_ID).creatorId(MANAGER_ID).build();
        Task t2 = Task.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).projectId(PROJECT_ID)
                .title("Task Two").assigneeId(OTHER_EMP_ID).creatorId(ADMIN_ID).build();

        User emp = User.builder().id(EMPLOYEE_ID).tenantId(TENANT_ID).fullName("kartik sah").employeeCode("GR0-EMP-001").role("EMPLOYEE").build();
        User mgr = User.builder().id(MANAGER_ID).tenantId(TENANT_ID).fullName("Shakti kapoor").employeeCode("GR0-MGR-008").role("MANAGER").build();
        User adm = User.builder().id(ADMIN_ID).tenantId(TENANT_ID).fullName("NITIN SAH").employeeCode("GR0-ADM-001").role("TENANT_ADMIN").build();
        Project proj = Project.builder().id(PROJECT_ID).tenantId(TENANT_ID).name("Core Platform").build();

        when(taskRepository.findByTenantId(eq(TENANT_ID), any())).thenReturn(new PageImpl<>(List.of(t1, t2)));
        when(taskRepository.findByTenantIdAndProjectId(eq(TENANT_ID), eq(PROJECT_ID), any())).thenReturn(new PageImpl<>(List.of(t1, t2)));
        when(userRepository.findAllById(any())).thenReturn(List.of(emp, mgr, adm));
        when(projectRepository.findAllById(any())).thenReturn(List.of(proj));

        // Manager visibility
        TenantContext.setContext(MANAGER_ID, TENANT_ID, "MANAGER");
        var mgrRes = taskService.getTasks(null, null, PageRequest.of(0, 20));
        assertEquals(2, mgrRes.getContent().size());
        assertEquals("kartik sah", mgrRes.getContent().get(0).getAssigneeName());
        assertEquals("Shakti kapoor", mgrRes.getContent().get(0).getCreatorName());
        assertEquals("Core Platform", mgrRes.getContent().get(0).getProjectName());

        // Admin visibility by project
        TenantContext.setContext(ADMIN_ID, TENANT_ID, "TENANT_ADMIN");
        var admRes = taskService.getTasks(PROJECT_ID, null, PageRequest.of(0, 20));
        assertEquals(2, admRes.getContent().size());
        verify(taskRepository).findByTenantIdAndProjectId(eq(TENANT_ID), eq(PROJECT_ID), any());
    }

    @Test
    void testEmployeeCannotReadOtherEmployeeTaskById() {
        UUID taskId = UUID.randomUUID();
        Task otherTask = Task.builder().id(taskId).tenantId(TENANT_ID).assigneeId(OTHER_EMP_ID).creatorId(MANAGER_ID).build();
        when(taskRepository.findByIdAndTenantId(taskId, TENANT_ID)).thenReturn(Optional.of(otherTask));

        TenantContext.setContext(EMPLOYEE_ID, TENANT_ID, "EMPLOYEE");
        assertThrows(AccessDeniedException.class, () -> taskService.getTaskForCurrentUser(taskId));

        TenantContext.setContext(OTHER_EMP_ID, TENANT_ID, "EMPLOYEE");
        Task allowed = taskService.getTaskForCurrentUser(taskId);
        assertEquals(taskId, allowed.getId());
    }

    @Test
    void testProjectMembersJoinExactUserDetails() {
        TenantContext.setContext(ADMIN_ID, TENANT_ID, "TENANT_ADMIN");

        ProjectMember m1 = ProjectMember.builder().id(UUID.randomUUID()).projectId(PROJECT_ID).tenantId(TENANT_ID).userId(MANAGER_ID).role("OWNER").build();
        ProjectMember m2 = ProjectMember.builder().id(UUID.randomUUID()).projectId(PROJECT_ID).tenantId(TENANT_ID).userId(EMPLOYEE_ID).role("MEMBER").build();
        User mgr = User.builder().id(MANAGER_ID).tenantId(TENANT_ID).fullName("Shakti kapoor").employeeCode("GR0-MGR-008").email("shakti@corp.com").role("MANAGER").build();
        User emp = User.builder().id(EMPLOYEE_ID).tenantId(TENANT_ID).fullName("NITIN SAH").employeeCode("GR0-EMP-002").email("nitin@corp.com").role("EMPLOYEE").build();

        when(projectMemberRepository.findByProjectIdAndTenantId(PROJECT_ID, TENANT_ID)).thenReturn(List.of(m1, m2));
        when(userRepository.findAllById(any())).thenReturn(List.of(mgr, emp));

        List<ProjectMemberDTO> members = projectService.getProjectMembers(PROJECT_ID);
        assertEquals(2, members.size());
        assertEquals("Shakti kapoor", members.get(0).getFullName());
        assertEquals("GR0-MGR-008", members.get(0).getEmployeeCode());
        assertNotNull(members.get(0).getUser());
        assertEquals("Shakti kapoor", members.get(0).getUser().getFullName());
        assertEquals("NITIN SAH", members.get(1).getFullName());
        assertEquals("MEMBER", members.get(1).getRole());
    }
}
