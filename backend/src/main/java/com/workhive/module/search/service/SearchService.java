package com.workhive.module.search.service;

import com.workhive.module.document.entity.Document;
import com.workhive.module.document.repository.DocumentRepository;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
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

    public SearchService(UserRepository userRepository,
                         ProjectRepository projectRepository,
                         TaskRepository taskRepository,
                         DocumentRepository documentRepository) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
    }

    public Map<String, Object> globalSearch(String query) {
        UUID tenantId = TenantContext.requireTenantId();
        Map<String, Object> results = new HashMap<>();

        if (query == null || query.isBlank()) {
            return results;
        }

        String q = query.trim();

        List<User> users = userRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();
        List<Project> projects = projectRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();
        List<Task> tasks = taskRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();
        List<Document> documents = documentRepository.searchByTenant(tenantId, q, PageRequest.of(0, 5)).getContent();

        results.put("users", users);
        results.put("projects", projects);
        results.put("tasks", tasks);
        results.put("documents", documents);

        return results;
    }
}
