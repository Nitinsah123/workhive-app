package com.workhive.module.export.service;

import com.opencsv.CSVWriter;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.activity.entity.WorkActivity;
import com.workhive.module.activity.repository.WorkActivityRepository;
import com.workhive.module.attendance.entity.Attendance;
import com.workhive.module.attendance.repository.AttendanceRepository;
import com.workhive.module.department.entity.Department;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.leave.entity.LeaveBalance;
import com.workhive.module.leave.entity.LeaveRequest;
import com.workhive.module.leave.entity.LeaveType;
import com.workhive.module.leave.repository.LeaveBalanceRepository;
import com.workhive.module.leave.repository.LeaveRequestRepository;
import com.workhive.module.leave.repository.LeaveTypeRepository;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.entity.ProjectMember;
import com.workhive.module.project.repository.ProjectMemberRepository;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.team.entity.Team;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExportService {

    private final TaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final WorkActivityRepository workActivityRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    public ExportService(TaskRepository taskRepository,
                         AttendanceRepository attendanceRepository,
                         UserRepository userRepository,
                         @Autowired(required = false) LeaveRequestRepository leaveRequestRepository,
                         @Autowired(required = false) LeaveTypeRepository leaveTypeRepository,
                         @Autowired(required = false) WorkActivityRepository workActivityRepository,
                         @Autowired(required = false) DepartmentRepository departmentRepository,
                         @Autowired(required = false) TeamRepository teamRepository,
                         @Autowired(required = false) ProjectRepository projectRepository,
                         @Autowired(required = false) ProjectMemberRepository projectMemberRepository,
                         @Autowired(required = false) LeaveBalanceRepository leaveBalanceRepository) {
        this.taskRepository = taskRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.workActivityRepository = workActivityRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    public ExportService(TaskRepository taskRepository,
                         AttendanceRepository attendanceRepository,
                         UserRepository userRepository) {
        this(taskRepository, attendanceRepository, userRepository, null, null, null, null, null, null, null, null);
    }

    public byte[] exportTasksCsv() throws Exception {
        UUID tenantId = TenantContext.requireTenantId();
        List<Task> tasks = taskRepository.findByTenantId(tenantId, Pageable.unpaged()).getContent();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"ID", "Title", "Priority", "Status", "Due Date", "Estimated Hours", "Actual Hours"});
            for (Task t : tasks) {
                writer.writeNext(new String[]{
                        t.getId().toString(),
                        t.getTitle(),
                        t.getPriority(),
                        t.getStatus(),
                        t.getDueDate() != null ? t.getDueDate().toString() : "",
                        t.getEstimatedHours() != null ? t.getEstimatedHours().toString() : "",
                        t.getActualHours() != null ? t.getActualHours().toString() : ""
                });
            }
        }
        return baos.toByteArray();
    }

    public byte[] exportAttendanceXlsx() throws Exception {
        UUID tenantId = TenantContext.requireTenantId();
        List<Attendance> attendances = attendanceRepository.findByTenantIdAndDateBetween(
                tenantId, LocalDate.now().minusDays(30), LocalDate.now());
        Map<UUID, User> userMap = userRepository.findAll().stream()
                .filter(u -> tenantId.equals(u.getTenantId()))
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Attendance");
            Row header = sheet.createRow(0);
            String[] columns = {"Employee Name", "Employee Code", "Date", "Check In", "Check Out", "Duration (Mins)", "Status", "Notes"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (Attendance a : attendances) {
                User u = userMap.get(a.getUserId());
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(u != null ? u.getFullName() : a.getUserId().toString());
                row.createCell(1).setCellValue(u != null && u.getEmployeeCode() != null ? u.getEmployeeCode() : "—");
                row.createCell(2).setCellValue(a.getDate().toString());
                row.createCell(3).setCellValue(a.getCheckIn() != null ? a.getCheckIn().toString() : "");
                row.createCell(4).setCellValue(a.getCheckOut() != null ? a.getCheckOut().toString() : "");
                row.createCell(5).setCellValue(a.getDurationMinutes() != null ? a.getDurationMinutes() : 0);
                row.createCell(6).setCellValue(a.getStatus());
                row.createCell(7).setCellValue(a.getNotes() != null ? a.getNotes() : "");
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] exportLeaveCsv() throws Exception {
        UUID tenantId = TenantContext.requireTenantId();
        List<LeaveRequest> leaves = leaveRequestRepository != null
                ? leaveRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, Pageable.unpaged()).getContent()
                : List.of();
        Map<UUID, User> userMap = userRepository.findAll().stream()
                .filter(u -> tenantId.equals(u.getTenantId()))
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        Map<UUID, String> typeMap = leaveTypeRepository != null
                ? leaveTypeRepository.findByTenantId(tenantId).stream().collect(Collectors.toMap(LeaveType::getId, LeaveType::getName, (a, b) -> a))
                : Map.of();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"ID", "Employee Name", "Employee Code", "Leave Type", "Start Date", "End Date", "Days", "Status", "Reason"});
            for (LeaveRequest lr : leaves) {
                User u = userMap.get(lr.getUserId());
                writer.writeNext(new String[]{
                        lr.getId().toString(),
                        u != null ? u.getFullName() : "",
                        u != null && u.getEmployeeCode() != null ? u.getEmployeeCode() : "",
                        typeMap.getOrDefault(lr.getLeaveTypeId(), "Leave"),
                        lr.getStartDate() != null ? lr.getStartDate().toString() : "",
                        lr.getEndDate() != null ? lr.getEndDate().toString() : "",
                        String.valueOf(lr.getDays()),
                        lr.getStatus(),
                        lr.getReason() != null ? lr.getReason() : ""
                });
            }
        }
        return baos.toByteArray();
    }

    public byte[] exportActivityCsv() throws Exception {
        UUID tenantId = TenantContext.requireTenantId();
        List<WorkActivity> activities = workActivityRepository != null
                ? workActivityRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, Pageable.ofSize(500)).getContent()
                : List.of();
        Map<UUID, User> userMap = userRepository.findAll().stream()
                .filter(u -> tenantId.equals(u.getTenantId()))
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.writeNext(new String[]{"ID", "Timestamp", "Employee Name", "Source", "Activity Type", "Title", "Description"});
            for (WorkActivity act : activities) {
                User u = act.getUserId() != null ? userMap.get(act.getUserId()) : null;
                writer.writeNext(new String[]{
                        act.getId().toString(),
                        act.getCreatedAt() != null ? act.getCreatedAt().toString() : "",
                        u != null ? u.getFullName() : "System",
                        act.getSource(),
                        act.getActivityType(),
                        act.getTitle() != null ? act.getTitle() : "",
                        act.getDescription() != null ? act.getDescription() : ""
                });
            }
        }
        return baos.toByteArray();
    }

    public byte[] exportEmployeeReportXlsx(UUID targetUserId) throws Exception {
        UUID tenantId = TenantContext.requireTenantId();
        UUID callerUserId = TenantContext.requireUserId();
        String role = TenantContext.getRole();
        UUID effectiveUserId = targetUserId != null ? targetUserId : callerUserId;

        // Strict RBAC: TENANT_ADMIN can download any employee report in tenant.
        // Employees/Managers may only download their own authorized report.
        if (!"TENANT_ADMIN".equalsIgnoreCase(role) && !callerUserId.equals(effectiveUserId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only Tenant Admins can export another employee's complete report");
        }

        User user = userRepository.findByIdAndTenantId(effectiveUserId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", effectiveUserId));

        String deptName = (departmentRepository != null && user.getDepartmentId() != null)
                ? departmentRepository.findById(user.getDepartmentId()).map(Department::getName).orElse("—")
                : "—";
        String teamName = (teamRepository != null && user.getTeamId() != null)
                ? teamRepository.findById(user.getTeamId()).map(Team::getName).orElse("—")
                : "—";
        String managerName = (user.getManagerId() != null)
                ? userRepository.findById(user.getManagerId()).map(User::getFullName).filter(s -> !s.isBlank()).orElse("N/A")
                : "N/A";

        List<Task> tasks = taskRepository != null
                ? taskRepository.findByTenantIdAndAssigneeId(tenantId, effectiveUserId, Pageable.unpaged()).getContent()
                : List.of();

        List<Attendance> attendances = attendanceRepository != null
                ? attendanceRepository.findByTenantIdAndUserIdAndDateBetween(
                        tenantId, effectiveUserId, LocalDate.now().minusDays(90), LocalDate.now())
                : List.of();

        List<LeaveBalance> leaveBalances = leaveBalanceRepository != null
                ? leaveBalanceRepository.findByTenantIdAndUserIdAndYear(
                        tenantId, effectiveUserId, LocalDate.now().getYear())
                : List.of();

        Map<UUID, String> typeMap = leaveTypeRepository != null
                ? leaveTypeRepository.findByTenantId(tenantId).stream().collect(Collectors.toMap(LeaveType::getId, LeaveType::getName, (a, b) -> a))
                : Map.of();

        List<ProjectMember> memberships = projectMemberRepository != null
                ? projectMemberRepository.findByUserIdAndTenantId(effectiveUserId, tenantId)
                : List.of();
        List<UUID> projectIds = memberships.stream().map(ProjectMember::getProjectId).toList();
        List<Project> projects = (projectRepository != null && !projectIds.isEmpty())
                ? projectRepository.findAllById(projectIds)
                : List.of();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Styling
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleStyle.setFont(titleFont);

            CellStyle subTitleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font subTitleFont = workbook.createFont();
            subTitleFont.setBold(true);
            subTitleStyle.setFont(subTitleFont);

            // ==================== SHEET 1: Summary ====================
            Sheet summarySheet = workbook.createSheet("Summary");

            Row r0 = summarySheet.createRow(0);
            Cell titleCell = r0.createCell(0);
            titleCell.setCellValue("WORKHIVE EMPLOYEE 360° SUMMARY");
            titleCell.setCellStyle(titleStyle);

            int r = 2;
            String[][] profileFields = {
                    {"Full Name", user.getFullName()},
                    {"Employee Code", user.getEmployeeCode() != null ? user.getEmployeeCode() : "N/A"},
                    {"Role", "TENANT_ADMIN".equalsIgnoreCase(user.getRole()) ? "Admin" : "MANAGER".equalsIgnoreCase(user.getRole()) ? "Manager" : "Employee"},
                    {"Status", user.getStatus() != null ? user.getStatus() : "ACTIVE"},
                    {"Email", user.getEmail()},
                    {"Phone", user.getPhone() != null && !user.getPhone().isBlank() ? user.getPhone() : "Not Provided"},
                    {"Department", deptName},
                    {"Team", teamName},
                    {"Reporting Manager", managerName},
                    {"Timezone", user.getTimezone() != null ? user.getTimezone() : "UTC"},
                    {"Generated Date", LocalDate.now().toString()}
            };

            for (String[] field : profileFields) {
                Row row = summarySheet.createRow(r++);
                Cell c0 = row.createCell(0);
                c0.setCellValue(field[0]);
                c0.setCellStyle(subTitleStyle);
                row.createCell(1).setCellValue(field[1]);
            }

            r++;
            Row kpiHeader = summarySheet.createRow(r++);
            Cell kpiTitle = kpiHeader.createCell(0);
            kpiTitle.setCellValue("KEY PERFORMANCE INDICATORS");
            kpiTitle.setCellStyle(subTitleStyle);

            long completedTasks = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus()) || "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
            double compRate = tasks.isEmpty() ? 0.0 : (completedTasks * 100.0 / tasks.size());
            long totalAttMins = attendances.stream().mapToLong(a -> a.getDurationMinutes() != null ? a.getDurationMinutes() : 0).sum();

            String[][] kpis = {
                    {"Total Assigned Tasks", String.valueOf(tasks.size())},
                    {"Completed Tasks", String.valueOf(completedTasks)},
                    {"Completion Rate", String.format("%.1f%%", compRate)},
                    {"Recorded Attendance Duration (Last 90 Days)", String.format("%.1f hrs", totalAttMins / 60.0)},
                    {"Active Projects", String.valueOf(projects.size())}
            };
            for (String[] kpi : kpis) {
                Row row = summarySheet.createRow(r++);
                row.createCell(0).setCellValue(kpi[0]);
                row.createCell(1).setCellValue(kpi[1]);
            }

            r++;
            Row lbHeader = summarySheet.createRow(r++);
            Cell lbTitle = lbHeader.createCell(0);
            lbTitle.setCellValue("LEAVE BALANCES (" + LocalDate.now().getYear() + ")");
            lbTitle.setCellStyle(subTitleStyle);

            Row lbCols = summarySheet.createRow(r++);
            String[] lbHeaders = {"Leave Type", "Total Allocated (Days)", "Used (Days)", "Remaining (Days)"};
            for (int i = 0; i < lbHeaders.length; i++) {
                Cell cell = lbCols.createCell(i);
                cell.setCellValue(lbHeaders[i]);
                cell.setCellStyle(headerStyle);
            }
            if (leaveBalances.isEmpty()) {
                Row row = summarySheet.createRow(r++);
                row.createCell(0).setCellValue("Standard Annual Leave");
                row.createCell(1).setCellValue("12");
                row.createCell(2).setCellValue("0");
                row.createCell(3).setCellValue("12");
            } else {
                for (LeaveBalance lb : leaveBalances) {
                    Row row = summarySheet.createRow(r++);
                    String typeName = lb.getLeaveTypeId() != null ? typeMap.getOrDefault(lb.getLeaveTypeId(), "Leave") : "Annual Leave";
                    row.createCell(0).setCellValue(typeName);
                    row.createCell(1).setCellValue(lb.getTotal());
                    row.createCell(2).setCellValue(lb.getUsed());
                    row.createCell(3).setCellValue(lb.getRemaining());
                }
            }
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);
            summarySheet.autoSizeColumn(2);
            summarySheet.autoSizeColumn(3);

            // ==================== SHEET 2: Attendance & Time Logs ====================
            Sheet attSheet = workbook.createSheet("Attendance & Time Logs");
            Row attHeader = attSheet.createRow(0);
            String[] attColumns = {"Date", "Check In", "Check Out", "Duration (Minutes)", "Duration (Hours)", "Status", "Notes"};
            for (int i = 0; i < attColumns.length; i++) {
                Cell cell = attHeader.createCell(i);
                cell.setCellValue(attColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            int attRowIdx = 1;
            for (Attendance a : attendances) {
                Row row = attSheet.createRow(attRowIdx++);
                row.createCell(0).setCellValue(a.getDate() != null ? a.getDate().toString() : "");
                row.createCell(1).setCellValue(a.getCheckIn() != null ? a.getCheckIn().toString() : "");
                row.createCell(2).setCellValue(a.getCheckOut() != null ? a.getCheckOut().toString() : "");
                long dur = a.getDurationMinutes() != null ? a.getDurationMinutes() : 0;
                row.createCell(3).setCellValue(dur);
                row.createCell(4).setCellValue(String.format("%.2f", dur / 60.0));
                row.createCell(5).setCellValue(a.getStatus() != null ? a.getStatus() : "");
                row.createCell(6).setCellValue(a.getNotes() != null ? a.getNotes() : "");
            }
            for (int i = 0; i < attColumns.length; i++) {
                attSheet.autoSizeColumn(i);
            }

            // ==================== SHEET 3: Projects & Tasks ====================
            Sheet projSheet = workbook.createSheet("Projects & Tasks");
            Row pTitleRow = projSheet.createRow(0);
            Cell pTitleCell = pTitleRow.createCell(0);
            pTitleCell.setCellValue("ASSIGNED PROJECTS (" + projects.size() + ")");
            pTitleCell.setCellStyle(subTitleStyle);

            Row pHeader = projSheet.createRow(1);
            String[] pColumns = {"Project Name", "Status", "Priority", "Project Role"};
            for (int i = 0; i < pColumns.length; i++) {
                Cell cell = pHeader.createCell(i);
                cell.setCellValue(pColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            int pRowIdx = 2;
            for (Project p : projects) {
                String memberRole = memberships.stream()
                        .filter(m -> m.getProjectId().equals(p.getId()))
                        .map(ProjectMember::getRole)
                        .findFirst().orElse("MEMBER");
                Row row = projSheet.createRow(pRowIdx++);
                row.createCell(0).setCellValue(p.getName());
                row.createCell(1).setCellValue(p.getStatus());
                row.createCell(2).setCellValue(p.getPriority() != null ? p.getPriority() : "NORMAL");
                row.createCell(3).setCellValue(memberRole);
            }

            pRowIdx++;
            Row tTitleRow = projSheet.createRow(pRowIdx++);
            Cell tTitleCell = tTitleRow.createCell(0);
            tTitleCell.setCellValue("TASK PORTFOLIO (" + tasks.size() + ")");
            tTitleCell.setCellStyle(subTitleStyle);

            Row tHeader = projSheet.createRow(pRowIdx++);
            String[] tColumns = {"Task Title", "Priority", "Status", "Estimated Hours", "Actual Hours", "Due Date"};
            for (int i = 0; i < tColumns.length; i++) {
                Cell cell = tHeader.createCell(i);
                cell.setCellValue(tColumns[i]);
                cell.setCellStyle(headerStyle);
            }
            for (Task t : tasks) {
                Row row = projSheet.createRow(pRowIdx++);
                row.createCell(0).setCellValue(t.getTitle());
                row.createCell(1).setCellValue(t.getPriority() != null ? t.getPriority() : "MEDIUM");
                row.createCell(2).setCellValue(t.getStatus() != null ? t.getStatus() : "TODO");
                row.createCell(3).setCellValue(t.getEstimatedHours() != null ? t.getEstimatedHours().toString() : "0");
                row.createCell(4).setCellValue(t.getActualHours() != null ? t.getActualHours().toString() : "0");
                row.createCell(5).setCellValue(t.getDueDate() != null ? t.getDueDate().toString() : "—");
            }
            for (int i = 0; i < tColumns.length; i++) {
                projSheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}
