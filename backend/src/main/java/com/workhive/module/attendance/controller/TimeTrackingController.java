package com.workhive.module.attendance.controller;

import com.workhive.module.attendance.dto.AttendanceDtos.TimeEntryRequest;
import com.workhive.module.attendance.entity.TimeEntry;
import com.workhive.module.attendance.service.TimeTrackingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = {"/api/time-entries", "/api/time"})
public class TimeTrackingController {

    private final TimeTrackingService timeTrackingService;

    public TimeTrackingController(TimeTrackingService timeTrackingService) {
        this.timeTrackingService = timeTrackingService;
    }

    @PostMapping
    public ResponseEntity<TimeEntry> logTime(@Valid @RequestBody TimeEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timeTrackingService.logTime(request));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<TimeEntry>> getMyTimeEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(timeTrackingService.getMyTimeEntries(PageRequest.of(page, size)));
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TimeEntry>> getTaskTimeEntries(@PathVariable UUID taskId) {
        return ResponseEntity.ok(timeTrackingService.getTaskTimeEntries(taskId));
    }
}
