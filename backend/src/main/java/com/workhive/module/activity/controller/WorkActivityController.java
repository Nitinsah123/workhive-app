package com.workhive.module.activity.controller;

import com.workhive.module.activity.entity.WorkActivity;
import com.workhive.module.activity.service.WorkActivityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
public class WorkActivityController {

    private final WorkActivityService workActivityService;

    public WorkActivityController(WorkActivityService workActivityService) {
        this.workActivityService = workActivityService;
    }

    @GetMapping
    public ResponseEntity<Page<WorkActivity>> getActivities(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(workActivityService.getActivities(userId, PageRequest.of(page, size)));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<WorkActivity>> getMyActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(workActivityService.getMyActivities(PageRequest.of(page, size)));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<WorkActivity>> getProjectActivities(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(workActivityService.getProjectActivities(projectId, PageRequest.of(page, size)));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<WorkActivityService.ActivityHeatmapResponse> getHeatmap(
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(workActivityService.getHeatmap(userId));
    }
}
