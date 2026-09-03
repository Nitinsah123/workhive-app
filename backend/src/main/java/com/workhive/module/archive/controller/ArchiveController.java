package com.workhive.module.archive.controller;

import com.workhive.module.archive.dto.ArchiveDtos.ArchiveSummaryResponse;
import com.workhive.module.archive.service.ArchiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/archive")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @GetMapping
    public ResponseEntity<ArchiveSummaryResponse> getArchiveSummary(
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(archiveService.getArchiveSummary(type));
    }

    @PostMapping("/restore/{type}/{id}")
    public ResponseEntity<Map<String, String>> restoreItem(
            @PathVariable String type,
            @PathVariable UUID id) {
        archiveService.restoreItem(type, id);
        return ResponseEntity.ok(Map.of("message", type + " restored successfully from archive"));
    }

    @DeleteMapping("/permanent/{type}/{id}")
    public ResponseEntity<Map<String, String>> permanentDeleteItem(
            @PathVariable String type,
            @PathVariable UUID id) {
        archiveService.permanentDeleteItem(type, id);
        return ResponseEntity.ok(Map.of("message", type + " permanently deleted"));
    }
}
