package com.workhive.module.task.controller;

import com.workhive.module.task.dto.TaskDtos.*;
import com.workhive.module.task.entity.*;
import com.workhive.module.task.service.TaskService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<Page<Task>> getTasks(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(taskService.getTasks(projectId, assigneeId, PageRequest.of(page, size)));
    }

    @GetMapping("/my-active")
    public ResponseEntity<List<Task>> getMyActiveTasks() {
        return ResponseEntity.ok(taskService.getMyActiveTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok(Map.of("message", "Task archived successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request));
    }

    @PostMapping("/{id}/submit-review")
    public ResponseEntity<Task> submitTaskForReview(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitTaskReviewRequest request) {
        return ResponseEntity.ok(taskService.submitTaskForReview(id, request));
    }

    @GetMapping("/{id}/submissions")
    public ResponseEntity<List<TaskSubmission>> getSubmissions(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getTaskSubmissions(id));
    }

    @RequestMapping(value = "/{id}/review", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Task> reviewTask(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewTaskRequest request) {
        return ResponseEntity.ok(taskService.reviewTask(id, request));
    }

    @GetMapping("/{id}/subtasks")
    public ResponseEntity<List<Subtask>> getSubtasks(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getSubtasks(id));
    }

    @PostMapping("/{id}/subtasks")
    public ResponseEntity<Subtask> addSubtask(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSubtaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.addSubtask(id, request));
    }

    @PatchMapping(value = {"/subtasks/{subtaskId}/toggle", "/{id}/subtasks/{subtaskId}/toggle"})
    public ResponseEntity<Subtask> toggleSubtask(@PathVariable UUID subtaskId) {
        return ResponseEntity.ok(taskService.toggleSubtask(subtaskId));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<Page<TaskComment>> getComments(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(taskService.getComments(id, PageRequest.of(page, size)));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TaskComment> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.addComment(id, request));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<TaskHistory>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getHistory(id));
    }
}
