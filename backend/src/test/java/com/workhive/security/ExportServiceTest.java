package com.workhive.security;

import com.workhive.module.attendance.repository.AttendanceRepository;
import com.workhive.module.export.service.ExportService;
import com.workhive.module.export.service.PdfExportService;
import com.workhive.module.leave.repository.LeaveBalanceRepository;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.repository.MilestoneRepository;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.report.dto.ReportDtos.*;
import com.workhive.module.report.service.ReportService;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.tenant.entity.Tenant;
import com.workhive.module.tenant.repository.TenantRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock private ReportService reportService;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private MilestoneRepository milestoneRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;

    private PdfExportService pdfExportService;
    private ExportService exportService;

    private final UUID TENANT_ID = UUID.randomUUID();
    private final UUID USER_ID = UUID.randomUUID();
    private final UUID PROJECT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pdfExportService = new PdfExportService(reportService, tenantRepository, userRepository,
                projectRepository, taskRepository, milestoneRepository, attendanceRepository, leaveBalanceRepository);

        exportService = new ExportService(taskRepository, attendanceRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGenerateEmployeeReportPdf_ProducesValidPdfBytes() throws Exception {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID);

        Tenant tenant = Tenant.builder().id(TENANT_ID).name("Acme Corp").code("ACM").build();
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).fullName("John Doe").employeeCode("ACM-EMP-001").role("EMPLOYEE").build();
        EmployeeWorkReport report = EmployeeWorkReport.builder()
                .employeeName("John Doe")
                .employeeCode("ACM-EMP-001")
                .assignedTasks(10)
                .completedTasks(8)
                .completionRate(80.0)
                .totalTimeLoggedMinutes(2400)
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(user));
        when(reportService.getEmployeeReport(USER_ID)).thenReturn(report);
        when(taskRepository.findByTenantIdAndAssigneeId(eq(TENANT_ID), eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        byte[] pdfBytes = pdfExportService.generateEmployeeReportPdf(USER_ID);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF should have substantial size");
        // PDF header check: %PDF-
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    void testGenerateProjectReportPdf_ProducesValidPdfBytes() throws Exception {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID);

        Tenant tenant = Tenant.builder().id(TENANT_ID).name("Acme Corp").build();
        Project project = Project.builder().id(PROJECT_ID).tenantId(TENANT_ID).name("Core SaaS Platform").status("ACTIVE").priority("HIGH").build();
        ProjectReport report = ProjectReport.builder()
                .projectName("Core SaaS Platform")
                .status("ACTIVE")
                .health("ON_TRACK")
                .progress(75)
                .totalTasks(20)
                .completedTasks(15)
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(projectRepository.findByIdAndTenantId(PROJECT_ID, TENANT_ID)).thenReturn(Optional.of(project));
        when(reportService.getProjectReport(PROJECT_ID)).thenReturn(report);
        when(taskRepository.findByTenantIdAndProjectId(eq(TENANT_ID), eq(PROJECT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        byte[] pdfBytes = pdfExportService.generateProjectReportPdf(PROJECT_ID);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    void testGenerateOrganizationReportPdf_ProducesValidPdfBytes() throws Exception {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID);

        Tenant tenant = Tenant.builder().id(TENANT_ID).name("Acme Corp").code("ACM").build();
        OrganizationReport report = OrganizationReport.builder()
                .organizationName("Acme Corp")
                .organizationCode("ACM")
                .totalEmployees(50)
                .activeProjects(10)
                .totalTasks(200)
                .completedTasks(160)
                .presentToday(45)
                .pendingLeaveRequests(3)
                .pendingTaskReviews(2)
                .totalDepartments(4)
                .totalTeams(8)
                .build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(reportService.getOrganizationReport()).thenReturn(report);

        byte[] pdfBytes = pdfExportService.generateOrganizationReportPdf();

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    void testExportTasksCsv_ProducesValidCsv() throws Exception {
        TenantContext.setTenantId(TENANT_ID);

        Task task = Task.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .title("Design Database Schema")
                .priority("HIGH")
                .status("COMPLETED")
                .estimatedHours(BigDecimal.valueOf(8))
                .actualHours(BigDecimal.valueOf(6))
                .build();

        when(taskRepository.findByTenantId(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        byte[] csvBytes = exportService.exportTasksCsv();

        assertNotNull(csvBytes);
        String csvContent = new String(csvBytes);
        assertTrue(csvContent.contains("Design Database Schema"));
        assertTrue(csvContent.contains("COMPLETED"));
        assertTrue(csvContent.contains("HIGH"));
    }

    @Test
    void testGenerateEmployeeReportPdf_AuthorizedAdmin_CanDownloadOtherEmployeeReport() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetEmployeeId = UUID.randomUUID();

        // Caller is TENANT_ADMIN
        TenantContext.setContext(adminId, TENANT_ID, "TENANT_ADMIN");

        Tenant tenant = Tenant.builder().id(TENANT_ID).name("Acme Corp").build();
        User employee = User.builder().id(targetEmployeeId).tenantId(TENANT_ID).fullName("Jane Engineer").email("jane@acme.internal").role("EMPLOYEE").build();

        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(userRepository.findByIdAndTenantId(targetEmployeeId, TENANT_ID)).thenReturn(Optional.of(employee));
        when(taskRepository.findByTenantIdAndAssigneeId(eq(TENANT_ID), eq(targetEmployeeId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        byte[] pdfBytes = pdfExportService.generateEmployeeReportPdf(targetEmployeeId);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    void testGenerateEmployeeReportPdf_UnauthorizedEmployee_BlockedFromOtherEmployeeReport() {
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();

        // Caller is regular EMPLOYEE A trying to download EMPLOYEE B's report
        TenantContext.setContext(employeeA, TENANT_ID, "EMPLOYEE");

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                pdfExportService.generateEmployeeReportPdf(employeeB));
    }

    @Test
    void testGenerateEmployeeReportPdf_CrossTenantAccess_Blocked() {
        UUID adminId = UUID.randomUUID();
        UUID foreignUserId = UUID.randomUUID();

        TenantContext.setContext(adminId, TENANT_ID, "TENANT_ADMIN");

        // Foreign user does not exist in TENANT_ID
        when(userRepository.findByIdAndTenantId(foreignUserId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(com.workhive.common.exception.ResourceNotFoundException.class, () ->
                pdfExportService.generateEmployeeReportPdf(foreignUserId));
    }
}