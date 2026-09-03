package com.workhive.module.project.service;

import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.project.dto.ProjectDtos.*;
import com.workhive.module.project.entity.Milestone;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.entity.ProjectMember;
import com.workhive.module.project.repository.MilestoneRepository;
import com.workhive.module.project.repository.ProjectMemberRepository;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final WorkActivityService workActivityService;
    private final AuditService auditService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository projectMemberRepository,
                          MilestoneRepository milestoneRepository,
                          TaskRepository taskRepository,
                          WorkActivityService workActivityService,
                          AuditService auditService) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.workActivityService = workActivityService;
        this.auditService = auditService;
    }

    public Page<Project> getProjects(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return projectRepository.findByTenantId(tenantId, pageable);
    }

    public Project getProject(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
    }

    @Transactional
    public Project createProject(CreateProjectRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Project project = Project.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .ownerId(userId)
                .managerId(request.getManagerId() != null ? request.getManagerId() : userId)
                .departmentId(request.getDepartmentId())
                .teamId(request.getTeamId())
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .status(request.getStatus() != null ? request.getStatus() : "PLANNING")
                .startDate(request.getStartDate())
                .targetDate(request.getTargetDate())
                .progress(0)
                .health("ON_TRACK")
                .build();

        project = projectRepository.save(project);

        // Add creator as member
        projectMemberRepository.save(ProjectMember.builder()
                .projectId(project.getId())
                .userId(userId)
                .tenantId(tenantId)
                .role("OWNER")
                .build());

        // Add requested members
        if (request.getMemberIds() != null) {
            for (UUID memberId : request.getMemberIds()) {
                if (!memberId.equals(userId) && !projectMemberRepository.existsByProjectIdAndUserIdAndTenantId(project.getId(), memberId, tenantId)) {
                    projectMemberRepository.save(ProjectMember.builder()
                            .projectId(project.getId())
                            .userId(memberId)
                            .tenantId(tenantId)
                            .role("MEMBER")
                            .build());
                }
            }
        }

        workActivityService.recordActivity(tenantId, userId, project.getId(), null, "WORKHIVE", "PROJECT_CREATED",
                "Created project: " + project.getName(), project.getDescription(), null, null);
        auditService.log(tenantId, userId, "PROJECT_CREATED", "PROJECT", project.getId(), null, null);

        return project;
    }

    @Transactional
    public Project updateProject(UUID id, UpdateProjectRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Project project = getProject(id);
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setManagerId(request.getManagerId());
        project.setDepartmentId(request.getDepartmentId());
        project.setTeamId(request.getTeamId());
        if (request.getPriority() != null) project.setPriority(request.getPriority());
        if (request.getStatus() != null) project.setStatus(request.getStatus());
        project.setStartDate(request.getStartDate());
        project.setTargetDate(request.getTargetDate());

        recalculateProgressAndHealth(project);
        project = projectRepository.save(project);

        auditService.log(tenantId, userId, "PROJECT_UPDATED", "PROJECT", project.getId(), null, null);
        return project;
    }

    @Transactional
    public void deleteProject(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Project project = getProject(id);
        project.setStatus("ARCHIVED");
        projectRepository.save(project);
        auditService.log(tenantId, userId, "PROJECT_ARCHIVED", "PROJECT", project.getId(), null, null);
    }

    @Transactional
    public void unarchiveProject(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Project project = getProject(id);
        project.setStatus("ACTIVE");
        projectRepository.save(project);
        auditService.log(tenantId, userId, "PROJECT_RESTORED", "PROJECT", project.getId(), null, null);
    }

    @Transactional
    public void permanentDeleteProject(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        String role = TenantContext.getRole();

        if (!"TENANT_ADMIN".equalsIgnoreCase(role)) {
            throw new com.workhive.common.exception.BadRequestException("Only Tenant Admins can permanently delete projects");
        }

        Project project = getProject(id);

        // 1. Clean task children for all tasks in this project
        entityManager.createNativeQuery("DELETE FROM task_comments WHERE task_id IN (SELECT id FROM tasks WHERE project_id = :pid)").setParameter("pid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM task_submissions WHERE task_id IN (SELECT id FROM tasks WHERE project_id = :pid)").setParameter("pid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM task_attachments WHERE task_id IN (SELECT id FROM tasks WHERE project_id = :pid)").setParameter("pid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM task_history WHERE task_id IN (SELECT id FROM tasks WHERE project_id = :pid)").setParameter("pid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM subtasks WHERE task_id IN (SELECT id FROM tasks WHERE project_id = :pid)").setParameter("pid", id).executeUpdate();

        // 2. Detach or clean time entries and activities
        entityManager.createNativeQuery("UPDATE time_entries SET project_id = NULL, task_id = NULL WHERE project_id = :pid").setParameter("pid", id).executeUpdate();
        entityManager.createNativeQuery("UPDATE work_activities SET project_id = NULL, task_id = NULL WHERE project_id = :pid").setParameter("pid", id).executeUpdate();

        // 3. Clean project members, tasks, and milestones
        entityManager.createNativeQuery("DELETE FROM tasks WHERE project_id = :pid").setParameter("pid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM milestones WHERE project_id = :pid").setParameter("pid", id).executeUpdate();
        entityManager.createNativeQuery("DELETE FROM project_members WHERE project_id = :pid").setParameter("pid", id).executeUpdate();

        projectRepository.delete(project);
        auditService.log(tenantId, userId, "PROJECT_PERMANENTLY_DELETED", "PROJECT", id, null, null);
    }

    public List<ProjectMember> getProjectMembers(UUID projectId) {
        UUID tenantId = TenantContext.requireTenantId();
        return projectMemberRepository.findByProjectIdAndTenantId(projectId, tenantId);
    }

    @Transactional
    public void addProjectMember(UUID projectId, UUID memberId, String role) {
        UUID tenantId = TenantContext.requireTenantId();
        if (!projectMemberRepository.existsByProjectIdAndUserIdAndTenantId(projectId, memberId, tenantId)) {
            projectMemberRepository.save(ProjectMember.builder()
                    .projectId(projectId)
                    .userId(memberId)
                    .tenantId(tenantId)
                    .role(role != null ? role : "MEMBER")
                    .build());
        }
    }

    @Transactional
    public void removeProjectMember(UUID projectId, UUID memberId) {
        UUID tenantId = TenantContext.requireTenantId();
        projectMemberRepository.deleteByProjectIdAndUserIdAndTenantId(projectId, memberId, tenantId);
    }

    public List<Milestone> getMilestones(UUID projectId) {
        UUID tenantId = TenantContext.requireTenantId();
        return milestoneRepository.findByProjectIdAndTenantId(projectId, tenantId);
    }

    @Transactional
    public Milestone createMilestone(UUID projectId, CreateMilestoneRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Milestone milestone = Milestone.builder()
                .projectId(projectId)
                .tenantId(tenantId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .targetDate(request.getTargetDate())
                .status("PENDING")
                .build();
        return milestoneRepository.save(milestone);
    }

    @Transactional
    public Milestone completeMilestone(UUID milestoneId) {
        UUID tenantId = TenantContext.requireTenantId();
        Milestone milestone = milestoneRepository.findByIdAndTenantId(milestoneId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", milestoneId));
        milestone.setStatus("COMPLETED");
        milestone.setCompletedAt(Instant.now());
        return milestoneRepository.save(milestone);
    }

    @Transactional
    public void recalculateProgressAndHealth(Project project) {
        long totalTasks = taskRepository.countByTenantIdAndProjectId(project.getTenantId(), project.getId());
        if (totalTasks == 0) {
            project.setProgress(0);
            project.setHealth("ON_TRACK");
            return;
        }

        long completedTasks = taskRepository.countByTenantIdAndProjectIdAndStatus(project.getTenantId(), project.getId(), "COMPLETED");
        int progress = (int) ((completedTasks * 100.0) / totalTasks);
        project.setProgress(progress);

        if ("COMPLETED".equals(project.getStatus()) || progress == 100) {
            project.setHealth("COMPLETED");
            return;
        }

        // Calculate health based on overdue tasks and target dates
        LocalDate today = LocalDate.now();
        List<Task> openTasks = taskRepository.findByTenantIdAndProjectIdAndStatus(project.getTenantId(), project.getId(), "TODO");
        openTasks.addAll(taskRepository.findByTenantIdAndProjectIdAndStatus(project.getTenantId(), project.getId(), "IN_PROGRESS"));
        openTasks.addAll(taskRepository.findByTenantIdAndProjectIdAndStatus(project.getTenantId(), project.getId(), "REVIEW"));

        long overdueCount = openTasks.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today)).count();

        if (overdueCount > 3 || (project.getTargetDate() != null && project.getTargetDate().isBefore(today) && progress < 100)) {
            project.setHealth("OFF_TRACK");
        } else if (overdueCount > 0 || (project.getTargetDate() != null && project.getTargetDate().minusDays(5).isBefore(today) && progress < 50)) {
            project.setHealth("AT_RISK");
        } else {
            project.setHealth("ON_TRACK");
        }
    }
}
