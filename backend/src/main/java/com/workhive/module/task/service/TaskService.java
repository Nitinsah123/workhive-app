package com.workhive.module.task.service;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.notification.service.NotificationService;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.project.service.ProjectService;
import com.workhive.module.task.dto.TaskDtos.*;
import com.workhive.module.task.entity.*;
import com.workhive.module.task.repository.*;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final TaskCommentRepository taskCommentRepository;
    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WorkActivityService workActivityService;
    private final AuditService auditService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public TaskService(TaskRepository taskRepository,
                       SubtaskRepository subtaskRepository,
                       TaskCommentRepository taskCommentRepository,
                       TaskHistoryRepository taskHistoryRepository,
                       TaskSubmissionRepository taskSubmissionRepository,
                       ProjectRepository projectRepository,
                       ProjectService projectService,
                       UserRepository userRepository,
                       NotificationService notificationService,
                       WorkActivityService workActivityService,
                       AuditService auditService) {
        this.taskRepository = taskRepository;
        this.subtaskRepository = subtaskRepository;
        this.taskCommentRepository = taskCommentRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.workActivityService = workActivityService;
        this.auditService = auditService;
    }

    public Page<Task> getTasks(UUID projectId, UUID assigneeId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (projectId != null) {
            return taskRepository.findByTenantIdAndProjectId(tenantId, projectId, pageable);
        }
        if (assigneeId != null) {
            return taskRepository.findByTenantIdAndAssigneeId(tenantId, assigneeId, pageable);
        }
        return taskRepository.findByTenantId(tenantId, pageable);
    }

    public List<Task> getMyActiveTasks() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return taskRepository.findActiveTasksByUser(tenantId, userId);
    }

    public Task getTask(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return taskRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }

    @Transactional
    public Task createTask(CreateTaskRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Task task = Task.builder()
                .tenantId(tenantId)
                .projectId(request.getProjectId())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .assigneeId(request.getAssigneeId())
                .creatorId(userId)
                .reviewerId(request.getReviewerId())
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .status(request.getStatus() != null ? request.getStatus() : "TODO")
                .dueDate(request.getDueDate())
                .labels(request.getLabels())
                .estimatedHours(request.getEstimatedHours())
                .actualHours(BigDecimal.ZERO)
                .milestoneId(request.getMilestoneId())
                .build();

        task = taskRepository.save(task);

        // Record history
        recordHistory(task.getId(), tenantId, userId, "CREATED", null, task.getTitle());

        // Create subtasks if provided
        if (request.getSubtasks() != null) {
            int order = 0;
            for (String sub : request.getSubtasks()) {
                if (sub != null && !sub.isBlank()) {
                    subtaskRepository.save(Subtask.builder()
                            .taskId(task.getId())
                            .tenantId(tenantId)
                            .title(sub.trim())
                            .completed(false)
                            .sortOrder(order++)
                            .build());
                }
            }
        }

        // Notify assignee
        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(userId)) {
            notificationService.createNotification(tenantId, task.getAssigneeId(), "TASK_ASSIGNED",
                    "New Task Assigned", "You were assigned task: " + task.getTitle(),
                    "TASK", task.getId(), "/tasks");
        }

        // Trigger project progress recalculation
        if (task.getProjectId() != null) {
            projectRepository.findByIdAndTenantId(task.getProjectId(), tenantId)
                    .ifPresent(projectService::recalculateProgressAndHealth);
        }

        workActivityService.recordActivity(tenantId, userId, task.getProjectId(), task.getId(), "WORKHIVE", "TASK_CREATED",
                "Created task: " + task.getTitle(), task.getDescription(), null, null);
        auditService.log(tenantId, userId, "TASK_CREATED", "TASK", task.getId(), null, null);

        return task;
    }

    @Transactional
    public Task updateTask(UUID id, UpdateTaskRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Task task = getTask(id);

        if (!task.getTitle().equals(request.getTitle())) {
            recordHistory(id, tenantId, userId, "title", task.getTitle(), request.getTitle());
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().equals(task.getDescription())) {
            recordHistory(id, tenantId, userId, "description", task.getDescription(), request.getDescription());
            task.setDescription(request.getDescription());
        }
        if (request.getAssigneeId() != null && !request.getAssigneeId().equals(task.getAssigneeId())) {
            recordHistory(id, tenantId, userId, "assignee", String.valueOf(task.getAssigneeId()), String.valueOf(request.getAssigneeId()));
            task.setAssigneeId(request.getAssigneeId());
            // Notify new assignee
            notificationService.createNotification(tenantId, request.getAssigneeId(), "TASK_ASSIGNED",
                    "Task Assigned", "You were assigned task: " + task.getTitle(),
                    "TASK", task.getId(), "/tasks");
        }
        if (request.getPriority() != null && !request.getPriority().equals(task.getPriority())) {
            recordHistory(id, tenantId, userId, "priority", task.getPriority(), request.getPriority());
            task.setPriority(request.getPriority());
        }
        if (request.getStatus() != null && !request.getStatus().equals(task.getStatus())) {
            recordHistory(id, tenantId, userId, "status", task.getStatus(), request.getStatus());
            task.setStatus(request.getStatus());
        }
        if (request.getDueDate() != null && !request.getDueDate().equals(task.getDueDate())) {
            recordHistory(id, tenantId, userId, "dueDate", String.valueOf(task.getDueDate()), String.valueOf(request.getDueDate()));
            task.setDueDate(request.getDueDate());
        }
        task.setReviewerId(request.getReviewerId());
        task.setLabels(request.getLabels());
        task.setEstimatedHours(request.getEstimatedHours());
        if (request.getActualHours() != null) task.setActualHours(request.getActualHours());
        task.setMilestoneId(request.getMilestoneId());

        task = taskRepository.save(task);

        if (task.getProjectId() != null) {
            projectRepository.findByIdAndTenantId(task.getProjectId(), tenantId)
                    .ifPresent(projectService::recalculateProgressAndHealth);
        }

        auditService.log(tenantId, userId, "TASK_UPDATED", "TASK", task.getId(), null, null);
        return task;
    }

    @Transactional
    public void deleteTask(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Task task = getTask(id);
        task.setStatus("ARCHIVED");
        taskRepository.save(task);

        if (task.getProjectId() != null) {
            projectRepository.findByIdAndTenantId(task.getProjectId(), tenantId)
                    .ifPresent(projectService::recalculateProgressAndHealth);
        }

        auditService.log(tenantId, userId, "TASK_ARCHIVED", "TASK", task.getId(), null, null);
    }

    @Transactional
    public void unarchiveTask(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Task task = getTask(id);
        task.setStatus("TODO");
        taskRepository.save(task);

        if (task.getProjectId() != null) {
            projectRepository.findByIdAndTenantId(task.getProjectId(), tenantId)
                    .ifPresent(projectService::recalculateProgressAndHealth);
        }

        auditService.log(tenantId, userId, "TASK_RESTORED", "TASK", task.getId(), null, null);
    }

    @Transactional
    public void permanentDeleteTask(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        String role = TenantContext.getRole();

        if (!"TENANT_ADMIN".equalsIgnoreCase(role)) {
            throw new com.workhive.common.exception.BadRequestException("Only Tenant Admins can permanently delete tasks");
        }

        Task task = getTask(id);

        entityManager.createNativeQuery("DELETE FROM task_comments WHERE task_id = :tid").setParameter("tid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM task_submissions WHERE task_id = :tid").setParameter("tid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM task_attachments WHERE task_id = :tid").setParameter("tid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM task_history WHERE task_id = :tid").setParameter("tid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM subtasks WHERE task_id = :tid").setParameter("tid", id).executeUpdate();
        entityManager.createNativeQuery("UPDATE time_entries SET task_id = NULL WHERE task_id = :tid").setParameter("tid", id).executeUpdate();
        entityManager.createNativeQuery("UPDATE work_activities SET task_id = NULL WHERE task_id = :tid").setParameter("tid", id).executeUpdate();

        taskRepository.delete(task);

        if (task.getProjectId() != null) {
            projectRepository.findByIdAndTenantId(task.getProjectId(), tenantId)
                    .ifPresent(projectService::recalculateProgressAndHealth);
        }

        auditService.log(tenantId, userId, "TASK_PERMANENTLY_DELETED", "TASK", id, null, null);
    }

    @Transactional
    public Task updateTaskStatus(UUID id, UpdateTaskStatusRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Task task = getTask(id);
        String oldStatus = task.getStatus();
        String newStatus = request.getStatus().toUpperCase();

        task.setStatus(newStatus);
        recordHistory(id, tenantId, userId, "status", oldStatus, newStatus);

        if (request.getComment() != null && !request.getComment().isBlank()) {
            taskCommentRepository.save(TaskComment.builder()
                    .taskId(id)
                    .tenantId(tenantId)
                    .authorId(userId)
                    .content(request.getComment())
                    .build());
        }

        task = taskRepository.save(task);

        // If moved to REVIEW, notify Action Center / Reviewer
        if ("REVIEW".equals(newStatus)) {
            User user = userRepository.findByIdAndTenantId(userId, tenantId).orElse(null);
            String userName = user != null ? user.getFullName() : "Employee";
            notificationService.notifyAdmins(tenantId, "TASK_REVIEW_SUBMITTED", "Task Submitted for Review",
                    userName + " submitted task for review: " + task.getTitle(),
                    "TASK", task.getId(), "/action-center");
        } else if ("COMPLETED".equals(newStatus)) {
            workActivityService.recordActivity(tenantId, userId, task.getProjectId(), task.getId(), "WORKHIVE", "TASK_COMPLETED",
                    "Completed task: " + task.getTitle(), null, null, null);
        }

        // Trigger project progress recalculation
        if (task.getProjectId() != null) {
            projectRepository.findByIdAndTenantId(task.getProjectId(), tenantId)
                    .ifPresent(projectService::recalculateProgressAndHealth);
        }

        auditService.log(tenantId, userId, "TASK_STATUS_" + newStatus, "TASK", task.getId(), null, null);
        return task;
    }

    @Transactional
    public Task submitTaskForReview(UUID id, SubmitTaskReviewRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Task task = getTask(id);

        // Security / validation: validate repository URL
        String repoUrl = validateRepositoryUrl(request.getRepositoryUrl());
        String provider = request.getProvider();
        if (provider == null || provider.isBlank()) {
            provider = detectProvider(repoUrl);
        }

        // Calculate version count
        long prevCount = taskSubmissionRepository.countByTaskIdAndTenantId(id, tenantId);

        TaskSubmission submission = TaskSubmission.builder()
                .tenantId(tenantId)
                .taskId(id)
                .projectId(task.getProjectId())
                .submittedBy(userId)
                .repositoryUrl(repoUrl)
                .provider(provider.toUpperCase())
                .branch(request.getBranch() != null && !request.getBranch().isBlank() ? request.getBranch().trim() : null)
                .pullRequestUrl(request.getPullRequestUrl() != null && !request.getPullRequestUrl().isBlank() ? request.getPullRequestUrl().trim() : null)
                .commitSha(request.getCommitSha() != null && !request.getCommitSha().isBlank() ? request.getCommitSha().trim() : null)
                .workSummary(request.getWorkSummary() != null ? request.getWorkSummary().trim() : "")
                .reviewStatus("PENDING")
                .version((int) (prevCount + 1))
                .build();

        taskSubmissionRepository.save(submission);

        // Update task status to REVIEW
        String oldStatus = task.getStatus();
        task.setStatus("REVIEW");
        recordHistory(id, tenantId, userId, "status", oldStatus, "REVIEW");
        recordHistory(id, tenantId, userId, "codebase_submission", null, repoUrl + (request.getBranch() != null ? " (" + request.getBranch() + ")" : ""));

        if (request.getWorkSummary() != null && !request.getWorkSummary().isBlank()) {
            taskCommentRepository.save(TaskComment.builder()
                    .taskId(id)
                    .tenantId(tenantId)
                    .authorId(userId)
                    .content("Submitted work with repository: " + repoUrl + "\nSummary: " + request.getWorkSummary())
                    .build());
        }

        task = taskRepository.save(task);

        // Notify Admins & Action Center
        User user = userRepository.findByIdAndTenantId(userId, tenantId).orElse(null);
        String userName = user != null ? user.getFullName() : "Employee";
        notificationService.notifyAdmins(tenantId, "TASK_REVIEW_SUBMITTED", "Task Submitted with Codebase",
                userName + " submitted task '" + task.getTitle() + "' with repository link: " + repoUrl,
                "TASK", task.getId(), "/action-center");

        workActivityService.recordActivity(tenantId, userId, task.getProjectId(), task.getId(), "WORKHIVE", "TASK_SUBMITTED",
                "Submitted task for review with codebase: " + repoUrl, request.getWorkSummary(), null, null);

        auditService.log(tenantId, userId, "TASK_SUBMITTED_WITH_CODEBASE", "TASK", task.getId(), null, null);
        return task;
    }

    public List<TaskSubmission> getTaskSubmissions(UUID taskId) {
        UUID tenantId = TenantContext.requireTenantId();
        return taskSubmissionRepository.findByTaskIdAndTenantIdOrderBySubmittedAtDesc(taskId, tenantId);
    }

    @Transactional
    public Task reviewTask(UUID id, ReviewTaskRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID reviewerId = TenantContext.requireUserId();

        Task task = getTask(id);
        String decision = request.getDecision().toUpperCase();

        if ("APPROVED".equals(decision)) {
            task.setStatus("COMPLETED");
            recordHistory(id, tenantId, reviewerId, "review", "REVIEW", "APPROVED (COMPLETED)");
        } else if ("CHANGES_REQUESTED".equals(decision)) {
            task.setStatus("IN_PROGRESS");
            recordHistory(id, tenantId, reviewerId, "review", "REVIEW", "CHANGES_REQUESTED (IN_PROGRESS)");
        } else {
            throw new BadRequestException("Invalid review decision. Must be APPROVED or CHANGES_REQUESTED");
        }

        // Update latest pending submission if exists
        taskSubmissionRepository.findFirstByTaskIdAndTenantIdAndReviewStatusOrderBySubmittedAtDesc(id, tenantId, "PENDING")
                .ifPresent(sub -> {
                    sub.setReviewStatus(decision);
                    sub.setReviewComment(request.getComment());
                    sub.setReviewedBy(reviewerId);
                    sub.setReviewedAt(java.time.Instant.now());
                    taskSubmissionRepository.save(sub);
                });

        if (request.getComment() != null && !request.getComment().isBlank()) {
            taskCommentRepository.save(TaskComment.builder()
                    .taskId(id)
                    .tenantId(tenantId)
                    .authorId(reviewerId)
                    .content("Review feedback (" + decision + "): " + request.getComment())
                    .build());
        }

        task = taskRepository.save(task);

        // Notify assignee
        if (task.getAssigneeId() != null) {
            notificationService.createNotification(tenantId, task.getAssigneeId(), "TASK_REVIEW_DECISION",
                    "Task Review: " + decision,
                    "Your task '" + task.getTitle() + "' was reviewed: " + decision +
                            (request.getComment() != null ? " - " + request.getComment() : ""),
                    "TASK", task.getId(), "/tasks");
        }

        if (task.getProjectId() != null) {
            projectRepository.findByIdAndTenantId(task.getProjectId(), tenantId)
                    .ifPresent(projectService::recalculateProgressAndHealth);
        }

        workActivityService.recordActivity(tenantId, reviewerId, task.getProjectId(), task.getId(), "WORKHIVE", "TASK_REVIEWED",
                "Reviewed task (" + decision + "): " + task.getTitle(), request.getComment(), null, null);

        auditService.log(tenantId, reviewerId, "TASK_REVIEW_" + decision, "TASK", task.getId(), null, null);
        return task;
    }

    private String validateRepositoryUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new BadRequestException("Repository URL cannot be blank");
        }
        String cleanUrl = url.trim();
        if (!cleanUrl.startsWith("https://")) {
            throw new BadRequestException("Repository URL must be a valid HTTPS URL (starting with https://)");
        }
        try {
            java.net.URI uri = new java.net.URI(cleanUrl);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new BadRequestException("Invalid repository host in URL");
            }
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme)) {
                throw new BadRequestException("Only secure HTTPS repository URLs are supported");
            }
        } catch (Exception e) {
            throw new BadRequestException("Invalid repository URL format: " + e.getMessage());
        }
        return cleanUrl;
    }

    private String detectProvider(String url) {
        String lower = url.toLowerCase();
        if (lower.contains("github.com")) return "GITHUB";
        if (lower.contains("gitlab.com") || lower.contains("gitlab")) return "GITLAB";
        if (lower.contains("bitbucket.org")) return "BITBUCKET";
        return "OTHER";
    }

    public List<Subtask> getSubtasks(UUID taskId) {
        UUID tenantId = TenantContext.requireTenantId();
        return subtaskRepository.findByTaskIdAndTenantIdOrderBySortOrder(taskId, tenantId);
    }

    @Transactional
    public Subtask addSubtask(UUID taskId, CreateSubtaskRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        long count = subtaskRepository.countByTaskIdAndTenantId(taskId, tenantId);
        Subtask subtask = Subtask.builder()
                .taskId(taskId)
                .tenantId(tenantId)
                .title(request.getTitle().trim())
                .completed(false)
                .sortOrder((int) count)
                .build();
        return subtaskRepository.save(subtask);
    }

    @Transactional
    public Subtask toggleSubtask(UUID subtaskId) {
        UUID tenantId = TenantContext.requireTenantId();
        Subtask subtask = subtaskRepository.findById(subtaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask", "id", subtaskId));
        if (!subtask.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Subtask", "id", subtaskId);
        }
        subtask.setCompleted(!subtask.getCompleted());
        return subtaskRepository.save(subtask);
    }

    public Page<TaskComment> getComments(UUID taskId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return taskCommentRepository.findByTaskIdAndTenantIdOrderByCreatedAtDesc(taskId, tenantId, pageable);
    }

    @Transactional
    public TaskComment addComment(UUID taskId, AddCommentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        TaskComment comment = TaskComment.builder()
                .taskId(taskId)
                .tenantId(tenantId)
                .authorId(userId)
                .content(request.getContent().trim())
                .build();
        comment = taskCommentRepository.save(comment);

        recordHistory(taskId, tenantId, userId, "comment", null, "Added comment");
        return comment;
    }

    public List<TaskHistory> getHistory(UUID taskId) {
        UUID tenantId = TenantContext.requireTenantId();
        return taskHistoryRepository.findByTaskIdAndTenantIdOrderByCreatedAtDesc(taskId, tenantId);
    }

    private void recordHistory(UUID taskId, UUID tenantId, UUID userId, String field, String oldValue, String newValue) {
        taskHistoryRepository.save(TaskHistory.builder()
                .taskId(taskId)
                .tenantId(tenantId)
                .userId(userId)
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }
}
