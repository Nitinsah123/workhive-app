package com.workhive.module.export.service;

import com.opencsv.CSVWriter;
import com.workhive.module.activity.entity.WorkActivity;
import com.workhive.module.activity.repository.WorkActivityRepository;
import com.workhive.module.attendance.entity.Attendance;
import com.workhive.module.attendance.repository.AttendanceRepository;
import com.workhive.module.leave.entity.LeaveRequest;
import com.workhive.module.leave.entity.LeaveType;
import com.workhive.module.leave.repository.LeaveRequestRepository;
import com.workhive.module.leave.repository.LeaveTypeRepository;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
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

    @Autowired
    public ExportService(TaskRepository taskRepository,
                         AttendanceRepository attendanceRepository,
                         UserRepository userRepository,
                         @Autowired(required = false) LeaveRequestRepository leaveRequestRepository,
                         @Autowired(required = false) LeaveTypeRepository leaveTypeRepository,
                         @Autowired(required = false) WorkActivityRepository workActivityRepository) {
        this.taskRepository = taskRepository;
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.workActivityRepository = workActivityRepository;
    }

    public ExportService(TaskRepository taskRepository,
                         AttendanceRepository attendanceRepository,
                         UserRepository userRepository) {
        this(taskRepository, attendanceRepository, userRepository, null, null, null);
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
}
