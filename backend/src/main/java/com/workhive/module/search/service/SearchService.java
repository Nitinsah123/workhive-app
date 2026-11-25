package com.workhive.module.search.service;

import com.workhive.module.department.entity.Department;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.document.entity.Document;
import com.workhive.module.document.repository.DocumentRepository;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.team.entity.Team;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SearchService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.workhive.module.task.service.TaskService taskService;

    public SearchService(UserRepository userRepository,
                         ProjectRepository projectRepository,
                         TaskRepository taskRepository,
                         DocumentRepository documentRepository,
                         DepartmentRepository departmentRepository,
                         TeamRepository teamRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
    }

    public Map<String, Object> globalSearch(String query) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        String role = TenantContext.getRole();
        boolean isPrivileged = "TENANT_ADMIN".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role)
                || "SUPER_ADMIN".equalsIgnoreCase(role)
                || "MANAGER".equalsIgnoreCase(role);
        Map<String, Object> results = new HashMap<>();

        if (query == null || query.isBlank()) {
            return results;
        }

        String q = query.trim();

        List<User> users = userRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();
        List<Project> projects = projectRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();
        List<Task> tasks = isPrivileged
                ? taskRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent()
                : taskRepository.searchByTenantAndAssignee(tenantId, userId, q, PageRequest.of(0, 5)).getContent();
        if (taskService != null && !tasks.isEmpty()) {
            taskService.enrichTasks(tenantId, tasks);
        }
        List<Document> documents = documentRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();
        List<Department> departments = departmentRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();
        List<Team> teams = teamRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();

        results.put("users", users);
        results.put("projects", projects);
        results.put("tasks", tasks);
        results.put("documents", documents);
        results.put("departments", departments);
        results.put("teams", teams);

        return results;
    }
}
