package com.workhive.module.attendance.controller;

import com.workhive.module.attendance.dto.AttendanceDtos.*;
import com.workhive.module.attendance.entity.Attendance;
import com.workhive.module.attendance.service.AttendanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    public ResponseEntity<Attendance> checkIn(@RequestBody(required = false) CheckInRequest request) {
        return ResponseEntity.ok(attendanceService.checkIn(request != null ? request : new CheckInRequest()));
    }

    @PostMapping("/check-out")
    public ResponseEntity<Attendance> checkOut(@RequestBody(required = false) CheckOutRequest request) {
        return ResponseEntity.ok(attendanceService.checkOut(request != null ? request : new CheckOutRequest()));
    }

    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> getTodayStatus() {
        Optional<Attendance> today = attendanceService.getTodayAttendance();
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("checkedIn", today.isPresent());
        response.put("status", today.map(Attendance::getStatus).orElse("NOT_CHECKED_IN"));
        response.put("record", today.orElse(null));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<Attendance>> getMyAttendance(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.getMyAttendance(PageRequest.of(page, size)));
    }

    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Page<Attendance>> getDailyAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(attendanceService.getTenantAttendance(date, PageRequest.of(page, size)));
    }
}
