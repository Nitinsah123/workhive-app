package com.workhive.security;

import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.document.repository.DocumentRepository;
import com.workhive.module.document.service.DocumentService;
import com.workhive.module.document.service.StorageService;
import com.workhive.module.integration.entity.Integration;
import com.workhive.module.integration.repository.IntegrationMappingRepository;
import com.workhive.module.integration.repository.IntegrationRepository;
import com.workhive.module.integration.service.IntegrationService;
import com.workhive.module.leave.dto.LeaveDtos.ReviewLeaveRequest;
import com.workhive.module.leave.repository.LeaveBalanceRepository;
import com.workhive.module.leave.repository.LeaveRequestRepository;
import com.workhive.module.leave.repository.LeaveTypeRepository;
import com.workhive.module.leave.service.LeaveService;
import com.workhive.module.notification.service.NotificationService;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.project.service.ProjectService;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.SubtaskRepository;
import com.workhive.module.task.repository.TaskCommentRepository;
import com.workhive.module.task.repository.TaskHistoryRepository;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.task.service.TaskService;
import com.workhive.module.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiTenantSecurityE2ETest {

    @Mock private TaskRepository taskRepository;
    @Mock private SubtaskRepository subtaskRepository;
    @Mock private TaskCommentRepository taskCommentRepository;
    @Mock private TaskHistoryRepository taskHistoryRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectService projectService;
    @Mock private UserRepository userRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private StorageService storageService;
    @Mock private IntegrationRepository integrationRepository;
    @Mock private IntegrationMappingRepository integrationMappingRepository;
    @Mock private NotificationService notificationService;
    @Mock private WorkActivityService workActivityService;
    @Mock private AuditService auditService;
    @Mock private com.workhive.module.task.repository.TaskSubmissionRepository taskSubmissionRepository;

    private TaskService taskService;
    private LeaveService leaveService;
    private DocumentService documentService;
    private IntegrationService integrationService;

    private final UUID TENANT_A = UUID.randomUUID();
    private final UUID TENANT_B = UUID.randomUUID();
    private final UUID USER_A = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, subtaskRepository, taskCommentRepository,
                taskHistoryRepository, taskSubmissionRepository, projectRepository, projectService, userRepository,
                notificationService, workActivityService, auditService);

        leaveService = new LeaveService(leaveRequestRepository, leaveBalanceRepository, leaveTypeRepository,
                userRepository, notificationService, workActivityService, auditService);

        documentService = new DocumentService(documentRepository, storageService, workActivityService, auditService);

        integrationService = new IntegrationService(integrationRepository, integrationMappingRepository,
                workActivityService, auditService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testTenantA_CannotReadTenantB_Tasks() {
        TenantContext.setTenantId(TENANT_A);
        TenantContext.setUserId(USER_A);

        UUID taskBId = UUID.randomUUID();
        when(taskRepository.findByIdAndTenantId(taskBId, TENANT_A)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTask(taskBId));
    }

    @Test
    void testTenantA_CannotApproveTenantB_Leave() {
        TenantContext.setTenantId(TENANT_A);
        TenantContext.setUserId(USER_A);

        UUID leaveBId = UUID.randomUUID();
        when(leaveRequestRepository.findByIdAndTenantId(leaveBId, TENANT_A)).thenReturn(Optional.empty());

        ReviewLeaveRequest reviewReq = new ReviewLeaveRequest();
        reviewReq.setStatus("APPROVED");

        assertThrows(ResourceNotFoundException.class, () -> leaveService.reviewLeave(leaveBId, reviewReq));
    }

    @Test
    void testTenantA_CannotAccessTenantB_Documents() {
        TenantContext.setTenantId(TENANT_A);
        TenantContext.setUserId(USER_A);

        UUID docBId = UUID.randomUUID();
        when(documentRepository.findByIdAndTenantId(docBId, TENANT_A)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> documentService.getDocument(docBId));
    }

    @Test
    void testTenantA_CannotDisconnectTenantB_Integration() {
        TenantContext.setTenantId(TENANT_A);
        TenantContext.setUserId(USER_A);

        UUID intBId = UUID.randomUUID();
        when(integrationRepository.findByIdAndTenantId(intBId, TENANT_A)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> integrationService.disconnect(intBId));
    }

    @Test
    void testStrictIsolation_TaskListingOnlyReturnsCurrentTenantData() {
        TenantContext.setTenantId(TENANT_A);
        TenantContext.setUserId(USER_A);

        Task taskA = Task.builder().id(UUID.randomUUID()).tenantId(TENANT_A).title("Task A").build();
        when(taskRepository.findByTenantId(eq(TENANT_A), nullable(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(taskA)));

        var result = taskService.getTasks(null, null, null);
        assertEquals(1, result.getContent().size());
        assertEquals(TENANT_A, result.getContent().get(0).getTenantId());
        verify(taskRepository).findByTenantId(eq(TENANT_A), nullable(Pageable.class));
    }
}