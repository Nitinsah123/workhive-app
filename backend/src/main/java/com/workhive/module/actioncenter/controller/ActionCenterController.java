package com.workhive.module.actioncenter.controller;

import com.workhive.module.actioncenter.dto.ActionCenterDto.*;
import com.workhive.module.actioncenter.service.ActionCenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/action-center")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
public class ActionCenterController {

    private final ActionCenterService actionCenterService;

    public ActionCenterController(ActionCenterService actionCenterService) {
        this.actionCenterService = actionCenterService;
    }

    @GetMapping
    public ResponseEntity<List<ActionItem>> getPendingItems(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(actionCenterService.getPendingItems(type));
    }

    @GetMapping("/summary")
    public ResponseEntity<ActionCenterSummary> getSummary() {
        return ResponseEntity.ok(actionCenterService.getSummary());
    }
}
