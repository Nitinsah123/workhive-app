package com.workhive.security;

import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.department.dto.DepartmentDtos.CreateDepartmentRequest;
import com.workhive.module.department.entity.Department;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.department.service.DepartmentService;
import com.workhive.module.project.dto.ProjectDtos.CreateProjectRequest;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.repository.MilestoneRepository;
import com.workhive.module.project.repository.ProjectMemberRepository;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.project.service.ProjectService;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.activity.service.WorkActivityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantIsolationTest {

    @Mock private DepartmentRepository departmentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private WorkActivityService workActivityService;
    @Mock private AuditService auditService;

    private DepartmentService departmentService;
    private ProjectService projectService;

    private final UUID TENANT_A = UUID.randomUUID();
    private final UUID TENANT_B = UUID.randomUUID();
    private final UUID USER_A = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        departmentService = new DepartmentService(departmentRepository, auditService);
        projectService = new ProjectService(projectRepository, projectMemberRepository,
                milestoneRepository, taskRepository, workActivityService, auditService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testTenantA_CannotAccessTenantB_Department() {
        // Authenticated as Tenant A
        TenantContext.setTenantId(TENANT_A);
        TenantContext.setUserId(USER_A);

        UUID departmentBId = UUID.randomUUID();
        // Repository returns empty when querying with Tenant A context
        when(departmentRepository.findByIdAndTenantId(departmentBId, TENANT_A)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> departmentService.getDepartment(departmentBId));
    }

    @Test
    void testTenantA_CannotAccessTenantB_Project() {
        // Authenticated as Tenant A
        TenantContext.setTenantId(TENANT_A);
        TenantContext.setUserId(USER_A);

        UUID projectBId = UUID.randomUUID();
        when(projectRepository.findByIdAndTenantId(projectBId, TENANT_A)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProject(projectBId));
    }

    @Test
    void testTenantA_CreationIsAlwaysScopedToTenantA() {
        TenantContext.setTenantId(TENANT_A);
        TenantContext.setUserId(USER_A);

        CreateDepartmentRequest req = new CreateDepartmentRequest();
        req.setName("Engineering");

        when(departmentRepository.existsByTenantIdAndName(TENANT_A, "Engineering")).thenReturn(false);
        Department savedDept = Department.builder().id(UUID.randomUUID()).tenantId(TENANT_A).name("Engineering").build();
        when(departmentRepository.save(any(Department.class))).thenReturn(savedDept);

        Department result = departmentService.createDepartment(req);

        assertEquals(TENANT_A, result.getTenantId());
        verify(departmentRepository).save(argThat(d -> d.getTenantId().equals(TENANT_A)));
    }
}
