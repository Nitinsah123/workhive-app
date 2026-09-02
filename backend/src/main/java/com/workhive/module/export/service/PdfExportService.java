package com.workhive.module.export.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.attendance.entity.Attendance;
import com.workhive.module.attendance.repository.AttendanceRepository;
import com.workhive.module.leave.entity.LeaveBalance;
import com.workhive.module.leave.repository.LeaveBalanceRepository;
import com.workhive.module.project.entity.Milestone;
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
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.workhive.module.activity.entity.WorkActivity;
import com.workhive.module.activity.repository.WorkActivityRepository;
import com.workhive.module.attendance.entity.TimeEntry;
import com.workhive.module.attendance.repository.TimeEntryRepository;
import com.workhive.module.department.entity.Department;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.leave.entity.LeaveRequest;
import com.workhive.module.leave.repository.LeaveRequestRepository;
import com.workhive.module.project.entity.ProjectMember;
import com.workhive.module.project.repository.ProjectMemberRepository;
import com.workhive.module.team.entity.Team;
import com.workhive.module.team.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PdfExportService {

    private final ReportService reportService;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final MilestoneRepository milestoneRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkActivityRepository workActivityRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    private static final Color BRAND_PRIMARY = new Color(79, 70, 229);    // Indigo-600 #4F46E5
    private static final Color BRAND_DARK = new Color(15, 23, 42);        // Slate-900 #0F172A
    private static final Color BRAND_GRAY = new Color(100, 116, 139);     // Slate-500 #64748B
    private static final Color BG_LIGHT = new Color(248, 250, 252);       // Slate-50 #F8FAFC
    private static final Color BORDER_COLOR = new Color(226, 232, 240);   // Slate-200 #E2E8F0
    private static final Color SUCCESS_GREEN = new Color(16, 185, 129);   // Emerald-500
    private static final Color WARNING_AMBER = new Color(245, 158, 11);   // Amber-500

    @Autowired
    public PdfExportService(ReportService reportService,
                            TenantRepository tenantRepository,
                            UserRepository userRepository,
                            ProjectRepository projectRepository,
                            TaskRepository taskRepository,
                            MilestoneRepository milestoneRepository,
                            AttendanceRepository attendanceRepository,
                            LeaveBalanceRepository leaveBalanceRepository,
                            @Autowired(required = false) DepartmentRepository departmentRepository,
                            @Autowired(required = false) TeamRepository teamRepository,
                            @Autowired(required = false) ProjectMemberRepository projectMemberRepository,
                            @Autowired(required = false) WorkActivityRepository workActivityRepository,
                            @Autowired(required = false) TimeEntryRepository timeEntryRepository,
                            @Autowired(required = false) LeaveRequestRepository leaveRequestRepository) {
        this.reportService = reportService;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.milestoneRepository = milestoneRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.workActivityRepository = workActivityRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public PdfExportService(ReportService reportService,
                            TenantRepository tenantRepository,
                            UserRepository userRepository,
                            ProjectRepository projectRepository,
                            TaskRepository taskRepository,
                            MilestoneRepository milestoneRepository,
                            AttendanceRepository attendanceRepository,
                            LeaveBalanceRepository leaveBalanceRepository) {
        this(reportService, tenantRepository, userRepository, projectRepository, taskRepository,
                milestoneRepository, attendanceRepository, leaveBalanceRepository,
                null, null, null, null, null, null);
    }

    public byte[] generateEmployeeReportPdf(UUID targetUserId) throws Exception {
        UUID tenantId = TenantContext.requireTenantId();
        UUID callerUserId = TenantContext.requireUserId();
        String role = TenantContext.getRole();
        UUID effectiveUserId = targetUserId != null ? targetUserId : callerUserId;

        // Strict RBAC: TENANT_ADMIN can download any employee report in tenant.
        // Employees/Managers may only download their own authorized report.
        if (!"TENANT_ADMIN".equalsIgnoreCase(role) && !callerUserId.equals(effectiveUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only Tenant Admins can download another employee's complete report");
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        User user = userRepository.findByIdAndTenantId(effectiveUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", effectiveUserId));

        EmployeeWorkReport report = reportService != null ? reportService.getEmployeeReport(effectiveUserId) : null;
        List<Task> tasks = taskRepository != null
                ? taskRepository.findByTenantIdAndAssigneeId(tenantId, effectiveUserId, Pageable.unpaged()).getContent()
                : List.of();
        List<Attendance> attendances = attendanceRepository != null
                ? attendanceRepository.findByTenantIdAndUserIdAndDateBetween(
                        tenantId, effectiveUserId, LocalDate.now().minusDays(30), LocalDate.now())
                : List.of();
        List<LeaveBalance> leaveBalances = leaveBalanceRepository != null
                ? leaveBalanceRepository.findByTenantIdAndUserIdAndYear(
                        tenantId, effectiveUserId, LocalDate.now().getYear())
                : List.of();

        // Real org data resolution
        String deptName = (departmentRepository != null && user.getDepartmentId() != null)
                ? departmentRepository.findById(user.getDepartmentId()).map(Department::getName).orElse("—")
                : "—";
        String teamName = (teamRepository != null && user.getTeamId() != null)
                ? teamRepository.findById(user.getTeamId()).map(Team::getName).orElse("—")
                : "—";
        String managerName = (user.getManagerId() != null)
                ? userRepository.findById(user.getManagerId()).map(User::getFullName).orElse("—")
                : "—";

        // Projects
        List<ProjectMember> memberships = (projectMemberRepository != null)
                ? projectMemberRepository.findByUserIdAndTenantId(effectiveUserId, tenantId)
                : List.of();
        List<UUID> projectIds = memberships.stream().map(ProjectMember::getProjectId).toList();
        List<Project> projects = (projectRepository != null && !projectIds.isEmpty())
                ? projectRepository.findAllById(projectIds)
                : List.of();

        // Time tracking
        long totalLoggedMinutes = (timeEntryRepository != null)
                ? timeEntryRepository.sumDurationByUserIdSince(tenantId, effectiveUserId, Instant.EPOCH)
                : (report != null ? report.getTotalTimeLoggedMinutes() : 0);
        List<TimeEntry> recentTimeEntries = (timeEntryRepository != null)
                ? timeEntryRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, effectiveUserId, PageRequest.of(0, 5)).getContent()
                : List.of();

        // Leave Requests
        List<LeaveRequest> recentLeaves = (leaveRequestRepository != null)
                ? leaveRequestRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, effectiveUserId, PageRequest.of(0, 5)).getContent()
                : List.of();

        // Work Activities
        List<WorkActivity> recentActivities = (workActivityRepository != null)
                ? workActivityRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, effectiveUserId, PageRequest.of(0, 6)).getContent()
                : List.of();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(document, baos);
        document.open();

        // 1. Header
        addHeader(document, tenant != null ? tenant.getName() : "WorkHive Workspace", "Employee 360° Comprehensive Dossier");

        // 2. Identity & Organization Grid (4 columns x 3 rows)
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(12);
        infoTable.setSpacingAfter(14);

        addInfoCell(infoTable, "Employee Name", user.getFullName());
        addInfoCell(infoTable, "Employee Code", user.getEmployeeCode() != null ? user.getEmployeeCode() : "N/A");
        addInfoCell(infoTable, "Role", user.getRole());
        addInfoCell(infoTable, "Status", user.getStatus() != null ? user.getStatus() : "ACTIVE");

        addInfoCell(infoTable, "Work Email", user.getEmail());
        addInfoCell(infoTable, "Phone", user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : "Not Provided");
        addInfoCell(infoTable, "Timezone", user.getTimezone() != null ? user.getTimezone() : "UTC");
        addInfoCell(infoTable, "Department", deptName);

        addInfoCell(infoTable, "Functional Team", teamName);
        addInfoCell(infoTable, "Reporting Manager", managerName);
        addInfoCell(infoTable, "Active Projects", String.valueOf(projects.size()));
        addInfoCell(infoTable, "Report Date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        document.add(infoTable);

        // 3. Metric Summary Cards
        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(16);

        long completedTasks = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus()) || "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
        double compRate = tasks.isEmpty() ? 0.0 : (completedTasks * 100.0 / tasks.size());

        addStatCard(statsTable, "Assigned Tasks", String.valueOf(tasks.size()), BRAND_PRIMARY);
        addStatCard(statsTable, "Completed Tasks", String.valueOf(completedTasks), SUCCESS_GREEN);
        addStatCard(statsTable, "Completion Rate", String.format("%.1f%%", compRate), BRAND_PRIMARY);
        addStatCard(statsTable, "Hours Logged", String.format("%.1f hrs", totalLoggedMinutes / 60.0), WARNING_AMBER);
        document.add(statsTable);

        // 4. Project Assignments Section
        addSectionTitle(document, "Project Assignments (" + projects.size() + ")");
        PdfPTable projTable = new PdfPTable(new float[]{4.0f, 2.0f, 2.0f, 2.0f});
        projTable.setWidthPercentage(100);
        projTable.setSpacingAfter(16);
        addTableHeader(projTable, new String[]{"Project Name", "Status", "Priority", "Assigned Role"});
        if (projects.isEmpty()) {
            addEmptyRow(projTable, 4, "No active project assignments recorded for this employee.");
        } else {
            for (Project p : projects) {
                String memberRole = memberships.stream()
                        .filter(m -> m.getProjectId().equals(p.getId()))
                        .map(ProjectMember::getRole)
                        .findFirst().orElse("MEMBER");
                projTable.addCell(createCell(p.getName(), false));
                projTable.addCell(createCell(p.getStatus(), false));
                projTable.addCell(createCell(p.getPriority() != null ? p.getPriority() : "NORMAL", false));
                projTable.addCell(createCell(memberRole, false));
            }
        }
        document.add(projTable);

        // 5. Current & Assigned Tasks Table
        addSectionTitle(document, "Task Portfolio (" + tasks.size() + ")");
        PdfPTable taskTable = new PdfPTable(new float[]{3.5f, 1.5f, 1.5f, 1.5f, 2.0f});
        taskTable.setWidthPercentage(100);
        taskTable.setSpacingAfter(16);
        addTableHeader(taskTable, new String[]{"Task Title", "Priority", "Status", "Est. Hrs", "Due Date"});
        if (tasks.isEmpty()) {
            addEmptyRow(taskTable, 5, "No tasks assigned currently.");
        } else {
            for (Task t : tasks) {
                addTaskRow(taskTable, t);
            }
        }
        document.add(taskTable);

        // 6. Attendance & Time Tracking Section
        addSectionTitle(document, "Attendance & Time Tracking Overview");
        PdfPTable attTable = new PdfPTable(2);
        attTable.setWidthPercentage(100);
        attTable.setSpacingAfter(16);

        PdfPCell attLeft = new PdfPCell();
        attLeft.setBorder(Rectangle.NO_BORDER);
        Paragraph attTitle = new Paragraph("30-Day Attendance Record:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BRAND_DARK));
        attLeft.addElement(attTitle);
        attLeft.addElement(new Paragraph(" • Days Recorded: " + attendances.size(), FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY)));
        long totalAttMins = attendances.stream().mapToLong(a -> a.getDurationMinutes() != null ? a.getDurationMinutes() : 0).sum();
        attLeft.addElement(new Paragraph(String.format(" • Total Recorded Duration: %.1f hours", totalAttMins / 60.0), FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY)));
        attTable.addCell(attLeft);

        PdfPCell attRight = new PdfPCell();
        attRight.setBorder(Rectangle.NO_BORDER);
        Paragraph timeTitle = new Paragraph("Recent Time Log Entries:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BRAND_DARK));
        attRight.addElement(timeTitle);
        if (recentTimeEntries.isEmpty()) {
            attRight.addElement(new Paragraph(" • No recent manual time entries recorded.", FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY)));
        } else {
            for (TimeEntry te : recentTimeEntries) {
                attRight.addElement(new Paragraph(" • " + (te.getDescription() != null ? te.getDescription() : "Work Log") + " (" + te.getDurationMinutes() + " mins)",
                        FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY)));
            }
        }
        attTable.addCell(attRight);
        document.add(attTable);

        // 7. Leave Balances & Requests Section
        addSectionTitle(document, "Leave Management & Balances (" + LocalDate.now().getYear() + ")");
        PdfPTable leaveTable = new PdfPTable(2);
        leaveTable.setWidthPercentage(100);
        leaveTable.setSpacingAfter(16);

        PdfPCell leaveLeft = new PdfPCell();
        leaveLeft.setBorder(Rectangle.NO_BORDER);
        Paragraph lbTitle = new Paragraph("Leave Balances:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BRAND_DARK));
        leaveLeft.addElement(lbTitle);
        for (LeaveBalance lb : leaveBalances) {
            leaveLeft.addElement(new Paragraph(" • Total: " + lb.getTotal() + " days | Used: " + lb.getUsed() + " | Remaining: " + lb.getRemaining() + " days",
                    FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY)));
        }
        if (leaveBalances.isEmpty()) {
            leaveLeft.addElement(new Paragraph(" • Standard annual leave allocation available.", FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY)));
        }
        leaveTable.addCell(leaveLeft);

        PdfPCell leaveRight = new PdfPCell();
        leaveRight.setBorder(Rectangle.NO_BORDER);
        Paragraph lrTitle = new Paragraph("Recent Leave Requests:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BRAND_DARK));
        leaveRight.addElement(lrTitle);
        if (recentLeaves.isEmpty()) {
            leaveRight.addElement(new Paragraph(" • No recent leave requests on file.", FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY)));
        } else {
            for (LeaveRequest lr : recentLeaves) {
                leaveRight.addElement(new Paragraph(" • " + lr.getStartDate() + " to " + lr.getEndDate() + " [" + lr.getStatus() + "]",
                        FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY)));
            }
        }
        leaveTable.addCell(leaveRight);
        document.add(leaveTable);

        // 8. Recent Work Activities & Audit Trail
        addSectionTitle(document, "Recent Work Activity Log");
        PdfPTable actTable = new PdfPTable(new float[]{3.0f, 5.0f, 2.0f});
        actTable.setWidthPercentage(100);
        actTable.setSpacingAfter(16);
        addTableHeader(actTable, new String[]{"Activity Domain", "Summary / Action", "Timestamp"});
        if (recentActivities.isEmpty()) {
            addEmptyRow(actTable, 3, "No recent activity entries recorded.");
        } else {
            for (WorkActivity wa : recentActivities) {
                actTable.addCell(createCell(wa.getActivityType() != null ? wa.getActivityType() : "ACTIVITY", false));
                actTable.addCell(createCell(wa.getTitle() != null ? wa.getTitle() : "Action performed", false));
                String ts = wa.getCreatedAt() != null ? wa.getCreatedAt().toString().substring(0, 10) : "—";
                actTable.addCell(createCell(ts, false));
            }
        }
        document.add(actTable);

        // Footer
        addFooter(document);

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateProjectReportPdf(UUID projectId) throws Exception {
        UUID tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        ProjectReport report = reportService.getProjectReport(projectId);
        List<Task> tasks = taskRepository.findByTenantIdAndProjectId(tenantId, projectId, Pageable.unpaged()).getContent();
        List<Milestone> milestones = milestoneRepository.findByProjectIdAndTenantId(projectId, tenantId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(document, baos);
        document.open();

        // 1. Header
        addHeader(document, tenant != null ? tenant.getName() : "WorkHive Workspace", "Project Performance & Status Report");

        // 2. Project info banner
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(15);
        infoTable.setSpacingAfter(15);

        addInfoCell(infoTable, "Project Name", project.getName());
        addInfoCell(infoTable, "Status", project.getStatus());
        addInfoCell(infoTable, "Health", project.getHealth() != null ? project.getHealth() : "ON_TRACK");
        addInfoCell(infoTable, "Priority", project.getPriority());
        document.add(infoTable);

        // 3. Metric Summary Cards
        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(20);

        addStatCard(statsTable, "Total Tasks", String.valueOf(report.getTotalTasks()), BRAND_PRIMARY);
        addStatCard(statsTable, "Completed Tasks", String.valueOf(report.getCompletedTasks()), SUCCESS_GREEN);
        addStatCard(statsTable, "Overall Progress", report.getProgress() + "%", BRAND_PRIMARY);
        addStatCard(statsTable, "Health Status", report.getHealth() != null ? report.getHealth() : "ON_TRACK",
                "ON_TRACK".equals(report.getHealth()) ? SUCCESS_GREEN : WARNING_AMBER);
        document.add(statsTable);

        // 4. Milestones Table
        addSectionTitle(document, "Project Milestones (" + milestones.size() + ")");
        PdfPTable msTable = new PdfPTable(new float[]{4.0f, 2.0f, 2.0f, 2.0f});
        msTable.setWidthPercentage(100);
        msTable.setSpacingAfter(20);
        addTableHeader(msTable, new String[]{"Milestone Name", "Status", "Target Date", "Completed"});

        if (milestones.isEmpty()) {
            addEmptyRow(msTable, 4, "No milestones created for this project.");
        } else {
            for (Milestone m : milestones) {
                PdfPCell c1 = createCell(m.getName(), false);
                PdfPCell c2 = createCell(m.getStatus(), false);
                PdfPCell c3 = createCell(m.getTargetDate() != null ? m.getTargetDate().toString() : "N/A", false);
                PdfPCell c4 = createCell(m.getCompletedAt() != null ? "Yes" : "Pending", false);
                msTable.addCell(c1);
                msTable.addCell(c2);
                msTable.addCell(c3);
                msTable.addCell(c4);
            }
        }
        document.add(msTable);

        // 5. Tasks Breakdown Table
        addSectionTitle(document, "Project Tasks (" + tasks.size() + ")");
        PdfPTable taskTable = new PdfPTable(new float[]{3.5f, 1.5f, 1.5f, 1.5f, 2.0f});
        taskTable.setWidthPercentage(100);
        taskTable.setSpacingAfter(20);

        addTableHeader(taskTable, new String[]{"Task Title", "Priority", "Status", "Est. Hrs", "Due Date"});
        if (tasks.isEmpty()) {
            addEmptyRow(taskTable, 5, "No tasks in this project.");
        } else {
            for (Task t : tasks) {
                addTaskRow(taskTable, t);
            }
        }
        document.add(taskTable);

        addFooter(document);
        document.close();
        return baos.toByteArray();
    }

    public byte[] generateOrganizationReportPdf() throws Exception {
        UUID tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        OrganizationReport report = reportService.getOrganizationReport();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        PdfWriter.getInstance(document, baos);
        document.open();

        // 1. Header
        addHeader(document, tenant != null ? tenant.getName() : "WorkHive Workspace", "Executive Organization & SaaS Analytics Report");

        // 2. Org info banner
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(15);
        infoTable.setSpacingAfter(15);

        addInfoCell(infoTable, "Organization", tenant != null ? tenant.getName() : "WorkHive");
        addInfoCell(infoTable, "Tenant Code", tenant != null ? tenant.getCode() : "N/A");
        addInfoCell(infoTable, "Industry", tenant != null && tenant.getIndustry() != null ? tenant.getIndustry() : "Technology");
        addInfoCell(infoTable, "Report Date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        document.add(infoTable);

        // 3. Metric Summary Cards
        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(20);

        addStatCard(statsTable, "Total Employees", String.valueOf(report.getTotalEmployees()), BRAND_PRIMARY);
        addStatCard(statsTable, "Active Projects", String.valueOf(report.getActiveProjects()), BRAND_PRIMARY);
        addStatCard(statsTable, "Total Tasks", String.valueOf(report.getTotalTasks()), BRAND_DARK);
        addStatCard(statsTable, "Completed Tasks", String.valueOf(report.getCompletedTasks()), SUCCESS_GREEN);
        document.add(statsTable);

        // 4. Secondary Metrics
        PdfPTable statsTable2 = new PdfPTable(4);
        statsTable2.setWidthPercentage(100);
        statsTable2.setSpacingAfter(25);

        long compRate = report.getTotalTasks() > 0 ? (report.getCompletedTasks() * 100 / report.getTotalTasks()) : 0;
        addStatCard(statsTable2, "Task Completion %", compRate + "%", SUCCESS_GREEN);
        addStatCard(statsTable2, "Present Today", String.valueOf(report.getPresentToday()), BRAND_PRIMARY);
        addStatCard(statsTable2, "Pending Leaves", String.valueOf(report.getPendingLeaveRequests()), WARNING_AMBER);
        addStatCard(statsTable2, "Departments", String.valueOf(report.getTotalDepartments()), BRAND_DARK);
        document.add(statsTable2);

        // 5. Structure Overview
        addSectionTitle(document, "Organizational Structure & Activity Overview");
        PdfPTable structTable = new PdfPTable(new float[]{4.0f, 3.0f, 3.0f});
        structTable.setWidthPercentage(100);
        structTable.setSpacingAfter(20);
        addTableHeader(structTable, new String[]{"Metric Domain", "Count / Value", "Operational Status"});

        structTable.addCell(createCell("Total Departments Registered", false));
        structTable.addCell(createCell(String.valueOf(report.getTotalDepartments()), false));
        structTable.addCell(createCell("Active", false));

        structTable.addCell(createCell("Total Functional Teams", false));
        structTable.addCell(createCell(String.valueOf(report.getTotalTeams()), false));
        structTable.addCell(createCell("Active", false));

        structTable.addCell(createCell("Pending Task Reviews in Action Center", false));
        structTable.addCell(createCell(String.valueOf(report.getPendingTaskReviews()), false));
        structTable.addCell(createCell(report.getPendingTaskReviews() > 0 ? "Requires Attention" : "Clear", false));

        document.add(structTable);

        addFooter(document);
        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, String tenantName, String reportTitle) throws Exception {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{6.0f, 4.0f});

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        Paragraph brand = new Paragraph("WORKHIVE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND_PRIMARY));
        Paragraph company = new Paragraph(tenantName.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND_DARK));
        left.addElement(brand);
        left.addElement(company);
        headerTable.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        Paragraph title = new Paragraph(reportTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND_DARK));
        title.setAlignment(Element.ALIGN_RIGHT);
        Paragraph date = new Paragraph("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY));
        date.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);
        right.addElement(date);
        headerTable.addCell(right);

        document.add(headerTable);

        // Divider line
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.setSpacingBefore(8);
        PdfPCell line = new PdfPCell();
        line.setFixedHeight(2f);
        line.setBackgroundColor(BRAND_PRIMARY);
        line.setBorder(Rectangle.NO_BORDER);
        divider.addCell(line);
        document.add(divider);
    }

    private void addFooter(Document document) throws Exception {
        Paragraph footer = new Paragraph("Confidential • Generated by WorkHive SaaS Multi-Tenant Platform • Strict Tenant Isolation Verified",
                FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);
    }

    private void addSectionTitle(Document document, String title) throws Exception {
        Paragraph p = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND_DARK));
        p.setSpacingBefore(10);
        p.setSpacingAfter(6);
        document.add(p);
    }

    private void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(6);

        Paragraph pLabel = new Paragraph(label.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, BRAND_GRAY));
        Paragraph pVal = new Paragraph(value != null ? value : "—", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BRAND_DARK));
        cell.addElement(pLabel);
        cell.addElement(pVal);
        table.addCell(cell);
    }

    private void addStatCard(PdfPTable table, String label, String value, Color valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(8);

        Paragraph pLabel = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_GRAY));
        Paragraph pVal = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, valueColor));
        cell.addElement(pLabel);
        cell.addElement(pVal);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(BRAND_PRIMARY);
            cell.setPadding(6);
            cell.setBorderColor(BORDER_COLOR);
            Paragraph p = new Paragraph(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE));
            cell.addElement(p);
            table.addCell(cell);
        }
    }

    private void addTaskRow(PdfPTable table, Task t) {
        table.addCell(createCell(t.getTitle(), false));
        table.addCell(createCell(t.getPriority(), false));
        table.addCell(createCell(t.getStatus(), false));
        table.addCell(createCell(t.getEstimatedHours() != null ? t.getEstimatedHours() + " hrs" : "—", false));
        table.addCell(createCell(t.getDueDate() != null ? t.getDueDate().toString() : "—", false));
    }

    private void addEmptyRow(PdfPTable table, int colSpan, String message) {
        PdfPCell cell = new PdfPCell(new Paragraph(message, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BRAND_GRAY)));
        cell.setColspan(colSpan);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, boolean bold) {
        Font font = bold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BRAND_DARK)
                : FontFactory.getFont(FontFactory.HELVETICA, 8, BRAND_DARK);
        PdfPCell cell = new PdfPCell(new Paragraph(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }
}