package com.workhive.module.report.controller;

import com.workhive.module.report.dto.ReportDtos.*;
import com.workhive.module.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/employee")
    public ResponseEntity<EmployeeWorkReport> getMyWorkReport(@RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(reportService.getEmployeeReport(userId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ProjectReport> getProjectReport(@PathVariable UUID projectId) {
        return ResponseEntity.ok(reportService.getProjectReport(projectId));
    }

    @GetMapping("/organization")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<OrganizationReport> getOrganizationReport() {
        return ResponseEntity.ok(reportService.getOrganizationReport());
    }
}
