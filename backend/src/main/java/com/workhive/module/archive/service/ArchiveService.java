package com.workhive.module.archive.service;

import com.workhive.common.exception.BadRequestException;
import com.workhive.module.archive.dto.ArchiveDtos.ArchiveSummaryResponse;
import com.workhive.module.archive.dto.ArchiveDtos.ArchivedItemDto;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.department.entity.Department;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.department.service.DepartmentService;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.project.service.ProjectService;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.task.service.TaskService;
import com.workhive.module.team.entity.Team;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.module.team.service.TeamService;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.module.user.service.UserService;
import com.workhive.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ArchiveService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    private final UserService userService;
    private final DepartmentService departmentService;
    private final TeamService teamService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final AuditService auditService;

    public ArchiveService(UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          TeamRepository teamRepository,
                          ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          UserService userService,
                          DepartmentService departmentService,
                          TeamService teamService,
                          ProjectService projectService,
                          TaskService taskService,
                          AuditService auditService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userService = userService;
        this.departmentService = departmentService;
        this.teamService = teamService;
        this.projectService = projectService;
        this.taskService = taskService;
        this.auditService = auditService;
    }

    public ArchiveSummaryResponse getArchiveSummary(String filterType) {
        UUID tenantId = TenantContext.requireTenantId();

        List<ArchivedItemDto> allItems = new ArrayList<>();

        // 1. Archived Users
        List<User> archivedUsers = userRepository.findByTenantIdAndStatus(tenantId, "ARCHIVED");
        for (User u : archivedUsers) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("email", u.getEmail());
            meta.put("role", u.getRole());
            meta.put("employeeCode", u.getEmployeeCode());
            allItems.add(ArchivedItemDto.builder()
                    .id(u.getId().toString())
                    .type("USER")
                    .title(u.getFullName())
                    .description(u.getEmail() + " • Role: " + u.getRole())
                    .status(u.getStatus())
                    .archivedAt(u.getUpdatedAt() != null ? u.getUpdatedAt() : u.getCreatedAt())
                    .metadata(meta)
                    .build());
        }

        // 2. Archived Departments
        List<Department> archivedDepts = departmentRepository.findByTenantIdAndStatus(tenantId, "ARCHIVED");
        for (Department d : archivedDepts) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("managerId", d.getManagerId() != null ? d.getManagerId().toString() : null);
            allItems.add(ArchivedItemDto.builder()
                    .id(d.getId().toString())
                    .type("DEPARTMENT")
                    .title(d.getName())
                    .description(d.getDescription() != null ? d.getDescription() : "Department")
                    .status(d.getStatus())
                    .archivedAt(d.getUpdatedAt() != null ? d.getUpdatedAt() : d.getCreatedAt())
                    .metadata(meta)
                    .build());
        }

        // 3. Archived Teams
        List<Team> archivedTeams = teamRepository.findByTenantIdAndStatus(tenantId, "ARCHIVED");
        for (Team t : archivedTeams) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("departmentId", t.getDepartmentId() != null ? t.getDepartmentId().toString() : null);
            meta.put("leadId", t.getLeadId() != null ? t.getLeadId().toString() : null);
            allItems.add(ArchivedItemDto.builder()
                    .id(t.getId().toString())
                    .type("TEAM")
                    .title(t.getName())
                    .description(t.getDescription() != null ? t.getDescription() : "Team Squad")
                    .status(t.getStatus())
                    .archivedAt(t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt())
                    .metadata(meta)
                    .build());
        }

        // 4. Archived Projects
        List<Project> archivedProjects = projectRepository.findByTenantIdAndStatus(tenantId, "ARCHIVED");
        for (Project p : archivedProjects) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("health", p.getHealth());
            meta.put("progress", p.getProgress());
            meta.put("priority", p.getPriority());
            allItems.add(ArchivedItemDto.builder()
                    .id(p.getId().toString())
                    .type("PROJECT")
                    .title(p.getName())
                    .description(p.getDescription() != null ? p.getDescription() : "Project")
                    .status(p.getStatus())
                    .archivedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                    .metadata(meta)
                    .build());
        }

        // 5. Archived Tasks
        List<Task> archivedTasks = taskRepository.findByTenantIdAndStatus(tenantId, "ARCHIVED");
        for (Task t : archivedTasks) {
            Map<String, Object> meta = new HashMap<>();
            meta.put("priority", t.getPriority());
            meta.put("dueDate", t.getDueDate() != null ? t.getDueDate().toString() : null);
            meta.put("projectId", t.getProjectId() != null ? t.getProjectId().toString() : null);
            allItems.add(ArchivedItemDto.builder()
                    .id(t.getId().toString())
                    .type("TASK")
                    .title(t.getTitle())
                    .description(t.getDescription() != null ? t.getDescription() : "Task")
                    .status(t.getStatus())
                    .archivedAt(t.getUpdatedAt() != null ? t.getUpdatedAt() : null)
                    .metadata(meta)
                    .build());
        }

        long usersCount = archivedUsers.size();
        long departmentsCount = archivedDepts.size();
        long teamsCount = archivedTeams.size();
        long projectsCount = archivedProjects.size();
        long tasksCount = archivedTasks.size();
        long totalCount = allItems.size();

        // Sort by archivedAt descending
        allItems.sort((a, b) -> {
            Instant t1 = a.getArchivedAt() != null ? a.getArchivedAt() : Instant.EPOCH;
            Instant t2 = b.getArchivedAt() != null ? b.getArchivedAt() : Instant.EPOCH;
            return t2.compareTo(t1);
        });

        List<ArchivedItemDto> filteredItems = allItems;
        if (filterType != null && !filterType.isBlank() && !"ALL".equalsIgnoreCase(filterType)) {
            filteredItems = allItems.stream()
                    .filter(item -> item.getType().equalsIgnoreCase(filterType.trim()))
                    .toList();
        }

        return ArchiveSummaryResponse.builder()
                .items(filteredItems)
                .totalCount(totalCount)
                .usersCount(usersCount)
                .departmentsCount(departmentsCount)
                .teamsCount(teamsCount)
                .projectsCount(projectsCount)
                .tasksCount(tasksCount)
                .build();
    }

    @Transactional
    public void restoreItem(String type, UUID id) {
        String normalizedType = type.toUpperCase().trim();
        switch (normalizedType) {
            case "USER":
                userService.unarchiveUser(id);
                break;
            case "DEPARTMENT":
                departmentService.unarchiveDepartment(id);
                break;
            case "TEAM":
                teamService.unarchiveTeam(id);
                break;
            case "PROJECT":
                projectService.unarchiveProject(id);
                break;
            case "TASK":
                taskService.unarchiveTask(id);
                break;
            default:
                throw new BadRequestException("Unsupported archive entity type: " + type);
        }
    }

    @Transactional
    public void permanentDeleteItem(String type, UUID id) {
        String normalizedType = type.toUpperCase().trim();
        switch (normalizedType) {
            case "USER":
                userService.permanentDeleteUser(id);
                break;
            case "DEPARTMENT":
                departmentService.permanentDeleteDepartment(id);
                break;
            case "TEAM":
                teamService.permanentDeleteTeam(id);
                break;
            case "PROJECT":
                projectService.permanentDeleteProject(id);
                break;
            case "TASK":
                taskService.permanentDeleteTask(id);
                break;
            default:
                throw new BadRequestException("Unsupported archive entity type: " + type);
        }
    }
}
