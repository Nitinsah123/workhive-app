package com.workhive.module.project.controller;

import com.workhive.module.project.dto.ProjectDtos.*;
import com.workhive.module.project.entity.Milestone;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.service.ProjectService;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;

    @Autowired
    public ProjectController(ProjectService projectService, @Autowired(required = false) TaskService taskService) {
        this.projectService = projectService;
        this.taskService = taskService;
    }

    public ProjectController(ProjectService projectService) {
        this(projectService, null);
    }

    @GetMapping
    public ResponseEntity<Page<Project>> getProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(projectService.getProjects(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Project> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Project> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> deleteProject(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(Map.of("message", "Project archived successfully"));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<ProjectMemberDTO>> getMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProjectMembers(id));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<Page<Task>> getProjectTasks(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        if (taskService == null) {
            return ResponseEntity.ok(Page.empty());
        }
        return ResponseEntity.ok(taskService.getTasks(id, assigneeId, PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> addMember(
            @PathVariable UUID id,
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "MEMBER") String role) {
        projectService.addProjectMember(id, userId, role);
        return ResponseEntity.ok(Map.of("message", "Member added to project"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        projectService.removeProjectMember(id, userId);
        return ResponseEntity.ok(Map.of("message", "Member removed from project"));
    }

    @GetMapping("/{id}/milestones")
    public ResponseEntity<List<Milestone>> getMilestones(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getMilestones(id));
    }

    @PostMapping("/{id}/milestones")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Milestone> createMilestone(
            @PathVariable UUID id,
            @Valid @RequestBody CreateMilestoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createMilestone(id, request));
    }

    @PatchMapping("/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Milestone> completeMilestone(@PathVariable UUID milestoneId) {
        return ResponseEntity.ok(projectService.completeMilestone(milestoneId));
    }
}
