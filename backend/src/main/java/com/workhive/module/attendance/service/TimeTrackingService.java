package com.workhive.module.attendance.service;

import com.workhive.module.attendance.dto.AttendanceDtos.TimeEntryRequest;
import com.workhive.module.attendance.entity.TimeEntry;
import com.workhive.module.attendance.repository.TimeEntryRepository;
import com.workhive.module.task.entity.Task;
import com.workhive.module.task.repository.TaskRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class TimeTrackingService {

    private final TimeEntryRepository timeEntryRepository;
    private final TaskRepository taskRepository;

    public TimeTrackingService(TimeEntryRepository timeEntryRepository, TaskRepository taskRepository) {
        this.timeEntryRepository = timeEntryRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public TimeEntry logTime(TimeEntryRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        TimeEntry entry = TimeEntry.builder()
                .tenantId(tenantId)
                .userId(userId)
                .taskId(request.getTaskId())
                .projectId(request.getProjectId())
                .durationMinutes(request.getDurationMinutes())
                .description(request.getDescription())
                .startedAt(request.getStartedAt())
                .endedAt(request.getEndedAt())
                .build();

        entry = timeEntryRepository.save(entry);

        // Update task actual hours if task is attached
        if (request.getTaskId() != null) {
            taskRepository.findByIdAndTenantId(request.getTaskId(), tenantId).ifPresent(task -> {
                long totalMinutes = timeEntryRepository.sumDurationByTaskId(tenantId, task.getId());
                BigDecimal hours = BigDecimal.valueOf(totalMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                task.setActualHours(hours);
                taskRepository.save(task);
            });
        }

        return entry;
    }

    public Page<TimeEntry> getMyTimeEntries(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return timeEntryRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId, pageable);
    }

    public List<TimeEntry> getTaskTimeEntries(UUID taskId) {
        UUID tenantId = TenantContext.requireTenantId();
        return timeEntryRepository.findByTenantIdAndTaskIdOrderByCreatedAtDesc(tenantId, taskId);
    }
}
