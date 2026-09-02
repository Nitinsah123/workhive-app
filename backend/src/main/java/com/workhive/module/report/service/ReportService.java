package com.workhive.module.report.service;

import com.workhive.module.activity.entity.WorkActivity;
import com.workhive.module.activity.repository.WorkActivityRepository;
import com.workhive.module.attendance.repository.AttendanceRepository;
import com.workhive.module.attendance.repository.TimeEntryRepository;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.leave.repository.LeaveRequestRepository;
import com.workhive.module.project.entity.Project;
import com.workhive.module.project.repository.MilestoneRepository;
import com.workhive.module.project.repository.ProjectMemberRepository;
import com.workhive.module.project.repository.ProjectRepository;
import com.workhive.module.report.dto.ReportDtos.*;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.module.tenant.entity.Tenant;
import com.workhive.module.tenant.repository.TenantRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkActivityRepository workActivityRepository;

    public ReportService(UserRepository userRepository,
                         TenantRepository tenantRepository,
                         DepartmentRepository departmentRepository,
                         TeamRepository teamRepository,
                         ProjectRepository projectRepository,
                         TaskRepository taskRepository,
                         AttendanceRepository attendanceRepository,
                         TimeEntryRepository timeEntryRepository,
                         LeaveRequestRepository leaveRequestRepository,
                         MilestoneRepository milestoneRepository,
                         ProjectMemberRepository projectMemberRepository,
                         WorkActivityRepository workActivityRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.attendanceRepository = attendanceRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.milestoneRepository = milestoneRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.workActivityRepository = workActivityRepository;
    }

    public EmployeeWorkReport getEmployeeReport(UUID targetUserId) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = targetUserId != null ? targetUserId : TenantContext.requireUserId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId).orElse(null);
        String userName = user != null ? user.getFullName() : "Employee";
        String empCode = user != null ? user.getEmployeeCode() : "";

        String deptName = user != null && user.getDepartmentId() != null
                ? departmentRepository.findByIdAndTenantId(user.getDepartmentId(), tenantId).map(d -> d.getName()).orElse("-")
                : "-";
        String teamName = user != null && user.getTeamId() != null
                ? teamRepository.findByIdAndTenantId(user.getTeamId(), tenantId).map(t -> t.getName()).orElse("-")
                : "-";

        List<Task> allUserTasks = taskRepository.findAll().stream()
                .filter(t -> tenantId.equals(t.getTenantId()) && userId.equals(t.getAssigneeId()))
                .toList();

        long assignedTasks = allUserTasks.size();
        long completedTasks = allUserTasks.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
        long inProgressTasks = allUserTasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long reviewTasks = allUserTasks.stream().filter(t -> "REVIEW".equalsIgnoreCase(t.getStatus())).count();

        LocalDate today = LocalDate.now();
        long overdueTasks = allUserTasks.stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()) && !"CANCELLED".equalsIgnoreCase(t.getStatus())
                        && t.getDueDate() != null && t.getDueDate().isBefore(today))
                .count();

        double completionRate = assignedTasks > 0 ? (completedTasks * 100.0) / assignedTasks : 0.0;

        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long timeLogged = timeEntryRepository.sumDurationByUserIdSince(tenantId, userId, thirtyDaysAgo);

        long daysPresent = attendanceRepository.findByTenantIdAndUserIdAndDateBetween(
                tenantId, userId, today.minusDays(30), today).size();

        long leaveDays = leaveRequestRepository.findApprovedLeavesInDateRange(
                tenantId, userId, today.minusDays(30), today).stream().mapToLong(l -> l.getDays()).sum();

        List<WorkActivity> recentActivities = workActivityRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(
                tenantId, userId, org.springframework.data.domain.PageRequest.of(0, 5)).getContent();

        return EmployeeWorkReport.builder()
                .employeeName(userName)
                .employeeCode(empCode)
                .department(deptName)
                .team(teamName)
                .assignedTasks(assignedTasks)
                .completedTasks(completedTasks)
                .inProgressTasks(inProgressTasks)
                .reviewTasks(reviewTasks)
                .overdueTasks(overdueTasks)
                .completionRate(Math.round(completionRate * 10.0) / 10.0)
                .totalTimeLoggedMinutes(timeLogged)
                .daysPresent(daysPresent)
                .leaveDaysTaken(leaveDays)
                .recentActivities(new ArrayList<>(recentActivities))
                .build();
    }

    public ProjectReport getProjectReport(UUID projectId) {
        UUID tenantId = TenantContext.requireTenantId();
        Project project = projectRepository.findByIdAndTenantId(projectId, tenantId).orElse(null);
        if (project == null) return null;

        List<Task> tasks = taskRepository.findAll().stream()
                .filter(t -> tenantId.equals(t.getTenantId()) && projectId.equals(t.getProjectId()))
                .toList();

        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus())).count();
        long todoTasks = tasks.stream().filter(t -> "TODO".equalsIgnoreCase(t.getStatus())).count();
        long inProgressTasks = tasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long reviewTasks = tasks.stream().filter(t -> "REVIEW".equalsIgnoreCase(t.getStatus())).count();

        LocalDate today = LocalDate.now();
        long overdueTasks = tasks.stream()
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()) && !"CANCELLED".equalsIgnoreCase(t.getStatus())
                        && t.getDueDate() != null && t.getDueDate().isBefore(today))
                .count();

        long totalMembers = projectMemberRepository.findByProjectIdAndTenantId(projectId, tenantId).size();
        long totalMilestones = milestoneRepository.findByProjectIdAndTenantId(projectId, tenantId).size();
        long completedMilestones = milestoneRepository.countByProjectIdAndTenantIdAndStatus(projectId, tenantId, "COMPLETED");

        return ProjectReport.builder()
                .projectName(project.getName())
                .status(project.getStatus())
                .health(project.getHealth())
                .progress(project.getProgress())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .todoTasks(todoTasks)
                .inProgressTasks(inProgressTasks)
                .reviewTasks(reviewTasks)
                .overdueTasks(overdueTasks)
                .totalMembers(totalMembers)
                .totalMilestones(totalMilestones)
                .completedMilestones(completedMilestones)
                .build();
    }

    public OrganizationReport getOrganizationReport() {
        UUID tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);

        long totalEmployees = userRepository.countByTenantId(tenantId);
        long totalDepartments = departmentRepository.countByTenantId(tenantId);
        long totalTeams = teamRepository.countByTenantId(tenantId);
        long activeProjects = projectRepository.countByTenantIdAndStatus(tenantId, "ACTIVE");
        long totalTasks = taskRepository.countByTenantId(tenantId);
        long completedTasks = taskRepository.countByTenantIdAndStatus(tenantId, "COMPLETED");
        long presentToday = attendanceRepository.countPresentToday(tenantId, LocalDate.now());
        long pendingLeaves = leaveRequestRepository.countPendingByTenant(tenantId);
        long pendingTaskReviews = taskRepository.countByTenantIdAndStatus(tenantId, "REVIEW");

        Map<String, Long> healthDist = new HashMap<>();
        for (Project p : projectRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())) {
            healthDist.put(p.getHealth(), healthDist.getOrDefault(p.getHealth(), 0L) + 1);
        }

        Map<String, Long> statusDist = new HashMap<>();
        statusDist.put("TODO", taskRepository.countByTenantIdAndStatus(tenantId, "TODO"));
        statusDist.put("IN_PROGRESS", taskRepository.countByTenantIdAndStatus(tenantId, "IN_PROGRESS"));
        statusDist.put("REVIEW", pendingTaskReviews);
        statusDist.put("COMPLETED", completedTasks);

        return OrganizationReport.builder()
                .organizationName(tenant != null ? tenant.getName() : "WorkHive Org")
                .organizationCode(tenant != null ? tenant.getCode() : "ORG")
                .totalEmployees(totalEmployees)
                .totalDepartments(totalDepartments)
                .totalTeams(totalTeams)
                .activeProjects(activeProjects)
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .presentToday(presentToday)
                .pendingLeaveRequests(pendingLeaves)
                .pendingTaskReviews(pendingTaskReviews)
                .projectHealthDistribution(healthDist)
                .taskStatusDistribution(statusDist)
                .build();
    }
}
