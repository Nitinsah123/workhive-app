package com.workhive.module.actioncenter.service;

import com.workhive.module.actioncenter.dto.ActionCenterDto.*;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.leave.entity.LeaveRequest;
import com.workhive.module.leave.entity.LeaveType;
import com.workhive.module.leave.repository.LeaveRequestRepository;
import com.workhive.module.leave.repository.LeaveTypeRepository;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ActionCenterService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final TaskRepository taskRepository;
    private final com.workhive.module.task.repository.TaskSubmissionRepository taskSubmissionRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;

    public ActionCenterService(LeaveRequestRepository leaveRequestRepository,
                               LeaveTypeRepository leaveTypeRepository,
                               TaskRepository taskRepository,
                               com.workhive.module.task.repository.TaskSubmissionRepository taskSubmissionRepository,
                               UserRepository userRepository,
                               DepartmentRepository departmentRepository,
                               TeamRepository teamRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.taskRepository = taskRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
    }

    public List<ActionItem> getPendingItems(String typeFilter) {
        UUID tenantId = TenantContext.requireTenantId();
        List<ActionItem> items = new ArrayList<>();

        // 1. Leave Requests
        if (typeFilter == null || typeFilter.isBlank() || "LEAVE".equalsIgnoreCase(typeFilter)) {
            List<LeaveRequest> pendingLeaves = leaveRequestRepository.findByTenantIdAndStatus(tenantId, "PENDING");
            for (LeaveRequest lr : pendingLeaves) {
                User requester = userRepository.findByIdAndTenantId(lr.getUserId(), tenantId).orElse(null);
                LeaveType leaveType = leaveTypeRepository.findByIdAndTenantId(lr.getLeaveTypeId(), tenantId).orElse(null);

                String deptName = requester != null && requester.getDepartmentId() != null
                        ? departmentRepository.findByIdAndTenantId(requester.getDepartmentId(), tenantId).map(d -> d.getName()).orElse(null)
                        : null;
                String teamName = requester != null && requester.getTeamId() != null
                        ? teamRepository.findByIdAndTenantId(requester.getTeamId(), tenantId).map(t -> t.getName()).orElse(null)
                        : null;

                Map<String, Object> meta = new HashMap<>();
                meta.put("leaveTypeName", leaveType != null ? leaveType.getName() : "Leave");
                meta.put("startDate", lr.getStartDate());
                meta.put("endDate", lr.getEndDate());
                meta.put("days", lr.getDays());
                meta.put("reason", lr.getReason());
                meta.put("supportingDocId", lr.getSupportingDocId());

                items.add(ActionItem.builder()
                        .id(lr.getId())
                        .type("LEAVE_REQUEST")
                        .title("Leave Request: " + (leaveType != null ? leaveType.getName() : "Leave") + " (" + lr.getDays() + " days)")
                        .description(lr.getReason())
                        .status(lr.getStatus())
                        .createdAt(lr.getCreatedAt())
                        .requesterId(requester != null ? requester.getId() : null)
                        .requesterName(requester != null ? requester.getFullName() : "Unknown")
                        .requesterEmail(requester != null ? requester.getEmail() : "")
                        .requesterEmployeeCode(requester != null ? requester.getEmployeeCode() : "")
                        .requesterAvatarUrl(requester != null ? requester.getAvatarUrl() : null)
                        .requesterDepartment(deptName)
                        .requesterTeam(teamName)
                        .entityType("LEAVE")
                        .entityId(lr.getId())
                        .metadata(meta)
                        .build());
            }
        }

        // 2. Task Reviews (Tasks in REVIEW status)
        if (typeFilter == null || typeFilter.isBlank() || "TASK".equalsIgnoreCase(typeFilter)) {
            List<Task> reviewTasks = taskRepository.findByTenantIdAndStatus(tenantId, "REVIEW");

            for (Task task : reviewTasks) {
                User assignee = task.getAssigneeId() != null
                        ? userRepository.findByIdAndTenantId(task.getAssigneeId(), tenantId).orElse(null)
                        : null;

                String deptName = assignee != null && assignee.getDepartmentId() != null
                        ? departmentRepository.findByIdAndTenantId(assignee.getDepartmentId(), tenantId).map(d -> d.getName()).orElse(null)
                        : null;
                String teamName = assignee != null && assignee.getTeamId() != null
                        ? teamRepository.findByIdAndTenantId(assignee.getTeamId(), tenantId).map(t -> t.getName()).orElse(null)
                        : null;

                Map<String, Object> meta = new HashMap<>();
                meta.put("projectId", task.getProjectId());
                meta.put("priority", task.getPriority());
                meta.put("dueDate", task.getDueDate());
                meta.put("estimatedHours", task.getEstimatedHours());

                // Populate latest codebase submission details if available
                taskSubmissionRepository.findFirstByTaskIdAndTenantIdOrderBySubmittedAtDesc(task.getId(), tenantId)
                        .ifPresent(sub -> {
                            meta.put("repositoryUrl", sub.getRepositoryUrl());
                            meta.put("provider", sub.getProvider());
                            meta.put("branch", sub.getBranch());
                            meta.put("pullRequestUrl", sub.getPullRequestUrl());
                            meta.put("commitSha", sub.getCommitSha());
                            meta.put("workSummary", sub.getWorkSummary());
                            meta.put("submissionVersion", sub.getVersion());
                            meta.put("submittedAt", sub.getSubmittedAt());
                        });

                items.add(ActionItem.builder()
                        .id(task.getId())
                        .type("TASK_REVIEW")
                        .title("Task Review: " + task.getTitle())
                        .description(task.getDescription())
                        .status(task.getStatus())
                        .createdAt(task.getUpdatedAt())
                        .requesterId(assignee != null ? assignee.getId() : task.getCreatorId())
                        .requesterName(assignee != null ? assignee.getFullName() : "Employee")
                        .requesterEmail(assignee != null ? assignee.getEmail() : "")
                        .requesterEmployeeCode(assignee != null ? assignee.getEmployeeCode() : "")
                        .requesterAvatarUrl(assignee != null ? assignee.getAvatarUrl() : null)
                        .requesterDepartment(deptName)
                        .requesterTeam(teamName)
                        .entityType("TASK")
                        .entityId(task.getId())
                        .metadata(meta)
                        .build());
            }
        }

        // Sort descending by creation/update date
        items.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return items;
    }

    public ActionCenterSummary getSummary() {
        UUID tenantId = TenantContext.requireTenantId();
        long pendingLeaves = leaveRequestRepository.countPendingByTenant(tenantId);
        long pendingTasks = taskRepository.countByTenantIdAndStatus(tenantId, "REVIEW");

        return ActionCenterSummary.builder()
                .totalPending(pendingLeaves + pendingTasks)
                .pendingLeaves(pendingLeaves)
                .pendingTaskReviews(pendingTasks)
                .pendingDocuments(0)
                .build();
    }
}
