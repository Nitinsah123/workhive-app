package com.workhive.module.attendance.dto;

import com.workhive.module.attendance.entity.Attendance;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AttendanceDtos {

    @Data
    public static class CheckInRequest {
        private String timezone;
        private String notes;
    }

    @Data
    public static class CheckOutRequest {
        private String notes;
    }

    @Data
    public static class TimeEntryRequest {
        private UUID taskId;
        private UUID projectId;
        @NotNull(message = "Duration in minutes is required")
        private Integer durationMinutes;
        private String description;
        private Instant startedAt;
        private Instant endedAt;
    }

    @Data
    public static class AttendanceStats {
        private long totalPresentToday;
        private long currentlyCheckedIn;
        private boolean userCheckedInToday;
        private Attendance currentAttendance;
    }

    public record AttendanceRecord(
            UUID id,
            UUID userId,
            String userName,
            String employeeCode,
            LocalDate date,
            Instant checkIn,
            Instant checkOut,
            Integer durationMinutes,
            String status,
            String notes
    ) {}
}
