package com.workhive.security;

import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.leave.dto.LeaveDtos.*;
import com.workhive.module.leave.entity.LeaveBalance;
import com.workhive.module.leave.entity.LeaveRequest;
import com.workhive.module.leave.entity.LeaveType;
import com.workhive.module.leave.repository.LeaveBalanceRepository;
import com.workhive.module.leave.repository.LeaveRequestRepository;
import com.workhive.module.leave.repository.LeaveTypeRepository;
import com.workhive.module.leave.service.LeaveService;
import com.workhive.module.notification.service.NotificationService;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.project.service.ProjectService;
import com.workhive.module.task.dto.TaskDtos.*;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.SubtaskRepository;
import com.workhive.module.task.repository.TaskCommentRepository;
import com.workhive.module.task.repository.TaskHistoryRepository;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.task.service.TaskService;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAndLeaveWorkflowTest {

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
    @Mock private NotificationService notificationService;
    @Mock private WorkActivityService workActivityService;
    @Mock private AuditService auditService;
    @Mock private com.workhive.module.task.repository.TaskSubmissionRepository taskSubmissionRepository;

    private TaskService taskService;
    private LeaveService leaveService;

    private final UUID TENANT_ID = UUID.randomUUID();
    private final UUID USER_ID = UUID.randomUUID();
    private final UUID MANAGER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, subtaskRepository, taskCommentRepository,
                taskHistoryRepository, taskSubmissionRepository, projectRepository, projectService, userRepository,
                notificationService, workActivityService, auditService);

        leaveService = new LeaveService(leaveRequestRepository, leaveBalanceRepository, leaveTypeRepository,
                userRepository, notificationService, workActivityService, auditService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testTaskReviewWorkflow_SubmitForReview_ThenApprove() {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID);

        UUID taskId = UUID.randomUUID();
        Task task = Task.builder()
                .id(taskId)
                .tenantId(TENANT_ID)
                .title("Implement Authentication")
                .status("IN_PROGRESS")
                .assigneeId(USER_ID)
                .build();

        when(taskRepository.findByIdAndTenantId(taskId, TENANT_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        // Employee moves task to REVIEW
        UpdateTaskStatusRequest reviewReq = new UpdateTaskStatusRequest();
        reviewReq.setStatus("REVIEW");
        Task reviewTask = taskService.updateTaskStatus(taskId, reviewReq);
        assertEquals("REVIEW", reviewTask.getStatus());

        // Manager reviews and approves task
        TenantContext.setUserId(MANAGER_ID);
        ReviewTaskRequest approveReq = new ReviewTaskRequest();
        approveReq.setDecision("APPROVED");
        approveReq.setComment("Excellent implementation and tests pass!");

        Task approvedTask = taskService.reviewTask(taskId, approveReq);
        assertEquals("COMPLETED", approvedTask.getStatus());
    }

    @Test
    void testTaskReviewWorkflow_ChangesRequested_MovesToInProgress() {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(MANAGER_ID);

        UUID taskId = UUID.randomUUID();
        Task task = Task.builder()
                .id(taskId)
                .tenantId(TENANT_ID)
                .title("Database Migration")
                .status("REVIEW")
                .assigneeId(USER_ID)
                .build();

        when(taskRepository.findByIdAndTenantId(taskId, TENANT_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        ReviewTaskRequest changeReq = new ReviewTaskRequest();
        changeReq.setDecision("CHANGES_REQUESTED");
        changeReq.setComment("Please add index on tenant_id column");

        Task result = taskService.reviewTask(taskId, changeReq);
        assertEquals("IN_PROGRESS", result.getStatus());
    }

    @Test
    void testLeaveWorkflow_Apply_DeductsFromBalanceOnApproval() {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID);

        UUID leaveTypeId = UUID.randomUUID();
        UUID leaveRequestId = UUID.randomUUID();

        LeaveType leaveType = LeaveType.builder().id(leaveTypeId).tenantId(TENANT_ID).name("Annual Leave").defaultBalance(20).build();
        LeaveBalance balance = LeaveBalance.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .leaveTypeId(leaveTypeId)
                .year(LocalDate.now().getYear())
                .total(20)
                .used(5)
                .remaining(15)
                .build();

        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).fullName("Alice Smith").build();

        when(leaveTypeRepository.findByIdAndTenantId(leaveTypeId, TENANT_ID)).thenReturn(Optional.of(leaveType));
        when(leaveBalanceRepository.findByTenantIdAndUserIdAndLeaveTypeIdAndYear(
                eq(TENANT_ID), eq(USER_ID), eq(leaveTypeId), eq(LocalDate.now().getYear())))
                .thenReturn(Optional.of(balance));
        // userRepository not strictly required for apply
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(i -> {
            LeaveRequest r = i.getArgument(0);
            r.setId(leaveRequestId);
            return r;
        });

        // 1. Employee applies for 3 days of leave
        ApplyLeaveRequest applyReq = new ApplyLeaveRequest();
        applyReq.setLeaveTypeId(leaveTypeId);
        applyReq.setStartDate(LocalDate.now().plusDays(2));
        applyReq.setEndDate(LocalDate.now().plusDays(4));
        applyReq.setReason("Personal vacation");

        LeaveRequest submitted = leaveService.applyLeave(applyReq);
        assertNotNull(submitted);
        assertEquals("PENDING", submitted.getStatus());
        assertEquals(3, submitted.getDays());

        // 2. Manager reviews and approves leave
        TenantContext.setUserId(MANAGER_ID);
        when(leaveRequestRepository.findByIdAndTenantId(leaveRequestId, TENANT_ID)).thenReturn(Optional.of(submitted));

        ReviewLeaveRequest reviewReq = new ReviewLeaveRequest();
        reviewReq.setStatus("APPROVED");
        reviewReq.setReviewComment("Approved, enjoy your vacation!");

        LeaveRequest approved = leaveService.reviewLeave(leaveRequestId, reviewReq);
        assertEquals("APPROVED", approved.getStatus());
        assertEquals(MANAGER_ID, approved.getReviewerId());

        // Verify balance was updated: used = 8, remaining = 12
        assertEquals(8, balance.getUsed());
        assertEquals(12, balance.getRemaining());
    }
}