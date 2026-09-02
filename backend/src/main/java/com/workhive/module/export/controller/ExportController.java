package com.workhive.module.export.controller;

import com.workhive.module.export.service.ExportService;
import com.workhive.module.export.service.PdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;
    private final PdfExportService pdfExportService;

    public ExportController(ExportService exportService, PdfExportService pdfExportService) {
        this.exportService = exportService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping({"/tasks/csv", "/csv/tasks"})
    public ResponseEntity<byte[]> exportTasksCsv() throws Exception {
        byte[] csv = exportService.exportTasksCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tasks-" + LocalDate.now() + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping({"/attendance/xlsx", "/attendance/excel", "/xlsx/attendance"})
    public ResponseEntity<byte[]> exportAttendanceXlsx() throws Exception {
        byte[] xlsx = exportService.exportAttendanceXlsx();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=attendance-" + LocalDate.now() + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @GetMapping({"/leave/csv", "/csv/leave"})
    public ResponseEntity<byte[]> exportLeaveCsv() throws Exception {
        byte[] csv = exportService.exportLeaveCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leave-report-" + LocalDate.now() + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping({"/activity/csv", "/csv/activity"})
    public ResponseEntity<byte[]> exportActivityCsv() throws Exception {
        byte[] csv = exportService.exportActivityCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=activity-report-" + LocalDate.now() + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping({"/employee/pdf", "/pdf/employee"})
    public ResponseEntity<byte[]> exportEmployeeReportPdf(@RequestParam(required = false) UUID userId) throws Exception {
        byte[] pdf = pdfExportService.generateEmployeeReportPdf(userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employee-report-" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping({"/project/{projectId}/pdf", "/pdf/project/{projectId}"})
    public ResponseEntity<byte[]> exportProjectReportPdf(@PathVariable UUID projectId) throws Exception {
        byte[] pdf = pdfExportService.generateProjectReportPdf(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=project-report-" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping({"/organization/pdf", "/pdf/organization"})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> exportOrganizationReportPdf() throws Exception {
        byte[] pdf = pdfExportService.generateOrganizationReportPdf();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=organization-report-" + LocalDate.now() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
