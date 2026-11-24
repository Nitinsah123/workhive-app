package com.workhive.module.attendance.service;

import com.workhive.common.exception.BadRequestException;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.attendance.dto.AttendanceDtos.*;
import com.workhive.module.attendance.entity.Attendance;
import com.workhive.module.attendance.repository.AttendanceRepository;
import com.workhive.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final WorkActivityService workActivityService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.workhive.module.activity.service.ActivityStreamService activityStreamService;

    public AttendanceService(AttendanceRepository attendanceRepository, WorkActivityService workActivityService) {
        this.attendanceRepository = attendanceRepository;
        this.workActivityService = workActivityService;
    }

    /**
     * Start a new active check-in session for today.
     * Allows punch-in at any time as long as no currently ACTIVE (open) session exists.
     */
    @Transactional
    public Attendance checkIn(CheckInRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        LocalDate today = LocalDate.now();

        // Check if there is already an active session (check_out is null)
        Optional<Attendance> activeSession = attendanceRepository
                .findFirstByTenantIdAndUserIdAndDateAndCheckOutIsNullOrderByCreatedAtDesc(tenantId, userId, today);
        if (activeSession.isPresent()) {
            throw new BadRequestException("You already have an active check-in session. Please check out before checking in again.");
        }

        Attendance attendance = Attendance.builder()
                .tenantId(tenantId)
                .userId(userId)
                .date(today)
                .checkIn(Instant.now())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .status("CHECKED_IN")
                .notes(request.getNotes())
                .build();

        attendance = attendanceRepository.save(attendance);

        workActivityService.recordActivity(tenantId, userId, null, null, "WORKHIVE", "ATTENDANCE_CHECKIN",
                "Checked in for work", null, null, null);

        if (activityStreamService != null) {
            try {
                activityStreamService.publishActivity(tenantId, userId, null, null,
                        "HEARTBEAT", "Attendance check-in logged", "{\"attendanceId\":\"" + attendance.getId() + "\"}");
            } catch (Exception ignored) {}
        }

        return attendance;
    }

    /**
     * Automatically create a check-in session on user login if no active session exists today.
     */
    @Transactional
    public void autoCheckInOnLogin(UUID tenantId, UUID userId, String timezone) {
        try {
            LocalDate today = LocalDate.now();
            Optional<Attendance> activeSession = attendanceRepository
                    .findFirstByTenantIdAndUserIdAndDateAndCheckOutIsNullOrderByCreatedAtDesc(tenantId, userId, today);

            if (activeSession.isEmpty()) {
                Attendance attendance = Attendance.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .date(today)
                        .checkIn(Instant.now())
                        .timezone(timezone != null ? timezone : "UTC")
                        .status("CHECKED_IN")
                        .notes("Auto check-in on login")
                        .build();

                attendanceRepository.save(attendance);

                workActivityService.recordActivity(tenantId, userId, null, null, "WORKHIVE", "ATTENDANCE_CHECKIN",
                        "Auto checked in on login", null, null, null);

                log.info("Auto checked in user {} for tenant {} on login", userId, tenantId);
            }
        } catch (Exception e) {
            log.warn("Auto check-in on login bypassed for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Close the current active check-in session, calculate elapsed minutes, and mark COMPLETED / CHECKED_OUT.
     */
    @Transactional
    public Attendance checkOut(CheckOutRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository
                .findFirstByTenantIdAndUserIdAndDateAndCheckOutIsNullOrderByCreatedAtDesc(tenantId, userId, today)
                .orElseThrow(() -> new BadRequestException("No active check-in session found for today to check out from"));

        Instant checkOutTime = Instant.now();
        int durationMinutes = (int) Duration.between(attendance.getCheckIn(), checkOutTime).toMinutes();

        attendance.setCheckOut(checkOutTime);
        attendance.setDurationMinutes(durationMinutes);
        attendance.setStatus("CHECKED_OUT");
        if (request.getNotes() != null) {
            attendance.setNotes(attendance.getNotes() != null ? attendance.getNotes() + " | " + request.getNotes() : request.getNotes());
        }

        attendance = attendanceRepository.save(attendance);

        workActivityService.recordActivity(tenantId, userId, null, null, "WORKHIVE", "ATTENDANCE_CHECKOUT",
                "Checked out from work (" + (durationMinutes / 60) + "h " + (durationMinutes % 60) + "m)", null, null, null);

        return attendance;
    }

    /**
     * Returns today's active session or latest completed session + summary for real-time UI.
     */
    public Map<String, Object> getTodayAttendanceStatus() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        LocalDate today = LocalDate.now();

        List<Attendance> todaySessions = attendanceRepository.findByTenantIdAndUserIdAndDateOrderByCreatedAtAsc(tenantId, userId, today);
        Attendance activeSession = todaySessions.stream()
                .filter(a -> a.getCheckOut() == null)
                .findFirst()
                .orElse(null);

        Attendance latestSession = !todaySessions.isEmpty() ? todaySessions.get(todaySessions.size() - 1) : null;

        int totalMinutesToday = 0;
        for (Attendance a : todaySessions) {
            if (a.getDurationMinutes() != null && a.getDurationMinutes() > 0) {
                totalMinutesToday += a.getDurationMinutes();
            } else if (a.getCheckIn() != null) {
                totalMinutesToday += (int) Math.max(0, Duration.between(a.getCheckIn(), Instant.now()).toMinutes());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("checkedIn", activeSession != null);
        response.put("status", activeSession != null ? "CHECKED_IN" : (!todaySessions.isEmpty() ? "CHECKED_OUT" : "NOT_CHECKED_IN"));
        response.put("record", activeSession != null ? activeSession : latestSession);
        response.put("sessions", todaySessions);
        response.put("totalMinutesToday", totalMinutesToday);
        return response;
    }

    public Optional<Attendance> getTodayAttendance() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return attendanceRepository.findFirstByTenantIdAndUserIdAndDateOrderByCreatedAtDesc(tenantId, userId, LocalDate.now());
    }

    public Page<Attendance> getMyAttendance(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return attendanceRepository.findByTenantIdAndUserIdOrderByDateDesc(tenantId, userId, pageable);
    }

    public Page<Attendance> getTenantAttendance(LocalDate date, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return attendanceRepository.findByTenantIdAndDateOrderByCreatedAtDesc(tenantId, targetDate, pageable);
    }

    public long getPresentTodayCount() {
        UUID tenantId = TenantContext.requireTenantId();
        return attendanceRepository.countPresentToday(tenantId, LocalDate.now());
    }
}
