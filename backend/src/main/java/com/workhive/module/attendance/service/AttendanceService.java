package com.workhive.module.attendance.service;

import com.workhive.common.exception.BadRequestException;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.attendance.dto.AttendanceDtos.*;
import com.workhive.module.attendance.entity.Attendance;
import com.workhive.module.attendance.repository.AttendanceRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkActivityService workActivityService;

    public AttendanceService(AttendanceRepository attendanceRepository, WorkActivityService workActivityService) {
        this.attendanceRepository = attendanceRepository;
        this.workActivityService = workActivityService;
    }

    @Transactional
    public Attendance checkIn(CheckInRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        LocalDate today = LocalDate.now();

        Optional<Attendance> existing = attendanceRepository.findByTenantIdAndUserIdAndDate(tenantId, userId, today);
        if (existing.isPresent()) {
            throw new BadRequestException("You have already checked in today");
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

        return attendance;
    }

    @Transactional
    public Attendance checkOut(CheckOutRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByTenantIdAndUserIdAndDate(tenantId, userId, today)
                .orElseThrow(() -> new BadRequestException("No check-in record found for today"));

        if ("CHECKED_OUT".equals(attendance.getStatus())) {
            throw new BadRequestException("You have already checked out today");
        }

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

    public Optional<Attendance> getTodayAttendance() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return attendanceRepository.findByTenantIdAndUserIdAndDate(tenantId, userId, LocalDate.now());
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
