package com.workhive.module.leave.controller;

import com.workhive.module.leave.dto.LeaveDtos.*;
import com.workhive.module.leave.entity.LeaveBalance;
import com.workhive.module.leave.entity.LeaveRequest;
import com.workhive.module.leave.entity.LeaveType;
import com.workhive.module.leave.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/leaves", "/api/leave"})
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/apply")
    public ResponseEntity<LeaveRequest> applyLeave(@Valid @RequestBody ApplyLeaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.applyLeave(request));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<LeaveRequest>> getMyLeaves(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(leaveService.getMyLeaves(PageRequest.of(page, size)));
    }

    @GetMapping("/my-balances")
    public ResponseEntity<List<LeaveBalance>> getMyBalances(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(leaveService.getMyBalances(year));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<Page<LeaveRequest>> getAllLeaves(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(leaveService.getAllLeaves(status, PageRequest.of(page, size)));
    }

    @RequestMapping(value = "/{id}/review", method = {RequestMethod.PATCH, RequestMethod.POST})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER')")
    public ResponseEntity<LeaveRequest> reviewLeave(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewLeaveRequest request) {
        return ResponseEntity.ok(leaveService.reviewLeave(id, request));
    }

    @GetMapping("/types")
    public ResponseEntity<List<LeaveType>> getLeaveTypes() {
        return ResponseEntity.ok(leaveService.getLeaveTypes());
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<LeaveType> createLeaveType(@Valid @RequestBody CreateLeaveTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.createLeaveType(request));
    }
}
