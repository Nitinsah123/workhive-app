package com.workhive.module.leave.service;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.leave.dto.LeaveDtos.*;
import com.workhive.module.leave.entity.LeaveBalance;
import com.workhive.module.leave.entity.LeaveRequest;
import com.workhive.module.leave.entity.LeaveType;
import com.workhive.module.leave.repository.LeaveBalanceRepository;
import com.workhive.module.leave.repository.LeaveRequestRepository;
import com.workhive.module.leave.repository.LeaveTypeRepository;
import com.workhive.module.notification.service.NotificationService;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WorkActivityService workActivityService;
    private final AuditService auditService;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        LeaveTypeRepository leaveTypeRepository,
                        UserRepository userRepository,
                        NotificationService notificationService,
                        WorkActivityService workActivityService,
                        AuditService auditService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.workActivityService = workActivityService;
        this.auditService = auditService;
    }

    @Transactional
    public LeaveRequest applyLeave(ApplyLeaveRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        // Calculate requested days (inclusive)
        int days = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        // Check for overlapping leaves
        long overlapping = leaveRequestRepository.countOverlappingRequests(tenantId, userId, request.getStartDate(), request.getEndDate());
        if (overlapping > 0) {
            throw new BadRequestException("You already have a pending or approved leave request for this date range");
        }

        // Verify leave type
        LeaveType leaveType = leaveTypeRepository.findByIdAndTenantId(request.getLeaveTypeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveType", "id", request.getLeaveTypeId()));

        // Check balance
        int currentYear = request.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByTenantIdAndUserIdAndLeaveTypeIdAndYear(tenantId, userId, leaveType.getId(), currentYear)
                .orElseGet(() -> {
                    // Initialize default balance
                    LeaveBalance newBal = LeaveBalance.builder()
                            .tenantId(tenantId)
                            .userId(userId)
                            .leaveTypeId(leaveType.getId())
                            .year(currentYear)
                            .total(leaveType.getDefaultBalance())
                            .used(0)
                            .remaining(leaveType.getDefaultBalance())
                            .build();
                    return leaveBalanceRepository.save(newBal);
                });

        if (balance.getRemaining() < days) {
            throw new BadRequestException("Insufficient leave balance. Requested: " + days + ", Remaining: " + balance.getRemaining());
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .tenantId(tenantId)
                .userId(userId)
                .leaveTypeId(leaveType.getId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .days(days)
                .reason(request.getReason())
                .status("PENDING")
                .supportingDocId(request.getSupportingDocId())
                .build();

        leaveRequest = leaveRequestRepository.save(leaveRequest);

        // Notify managers / admin
        User user = userRepository.findByIdAndTenantId(userId, tenantId).orElse(null);
        String userName = user != null ? user.getFullName() : "Employee";
        notificationService.notifyAdmins(tenantId, "LEAVE_SUBMITTED", "New Leave Request",
                userName + " applied for " + days + " days " + leaveType.getName(),
                "LEAVE", leaveRequest.getId(), "/action-center");

        // Record activity & audit
        workActivityService.recordActivity(tenantId, userId, null, null, "WORKHIVE", "LEAVE_APPLIED",
                "Applied for " + days + " days of " + leaveType.getName(), request.getReason(), null, null);
        auditService.log(tenantId, userId, "LEAVE_APPLIED", "LEAVE_REQUEST", leaveRequest.getId(), null, null);

        return leaveRequest;
    }

    @Transactional
    public LeaveRequest reviewLeave(UUID requestId, ReviewLeaveRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID reviewerId = TenantContext.requireUserId();

        LeaveRequest leaveRequest = leaveRequestRepository.findByIdAndTenantId(requestId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", requestId));

        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new BadRequestException("This leave request has already been reviewed");
        }

        String newStatus = request.getStatus().toUpperCase();
        if (!newStatus.equals("APPROVED") && !newStatus.equals("REJECTED")) {
            throw new BadRequestException("Invalid review status. Must be APPROVED or REJECTED");
        }

        if (newStatus.equals("REJECTED") && (request.getReviewComment() == null || request.getReviewComment().isBlank())) {
            throw new BadRequestException("Rejection reason is required");
        }

        leaveRequest.setStatus(newStatus);
        leaveRequest.setReviewerId(reviewerId);
        leaveRequest.setReviewComment(request.getReviewComment());
        leaveRequest.setReviewedAt(Instant.now());

        if ("APPROVED".equals(newStatus)) {
            // Deduct balance
            int currentYear = leaveRequest.getStartDate().getYear();
            LeaveBalance balance = leaveBalanceRepository.findByTenantIdAndUserIdAndLeaveTypeIdAndYear(
                    tenantId, leaveRequest.getUserId(), leaveRequest.getLeaveTypeId(), currentYear)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

            balance.setUsed(balance.getUsed() + leaveRequest.getDays());
            balance.setRemaining(Math.max(0, balance.getTotal() - balance.getUsed()));
            leaveBalanceRepository.save(balance);

            workActivityService.recordActivity(tenantId, leaveRequest.getUserId(), null, null, "WORKHIVE", "LEAVE_APPROVED",
                    "Leave request approved (" + leaveRequest.getDays() + " days)", request.getReviewComment(), null, null);
        }

        leaveRequest = leaveRequestRepository.save(leaveRequest);

        // Notify employee
        notificationService.createNotification(tenantId, leaveRequest.getUserId(), "LEAVE_REVIEWED",
                "Leave Request " + newStatus,
                "Your leave request for " + leaveRequest.getDays() + " days was " + newStatus.toLowerCase() +
                        (request.getReviewComment() != null ? ": " + request.getReviewComment() : ""),
                "LEAVE", leaveRequest.getId(), "/leave");

        auditService.log(tenantId, reviewerId, "LEAVE_" + newStatus, "LEAVE_REQUEST", leaveRequest.getId(), null, null);

        return leaveRequest;
    }

    public Page<LeaveRequest> getMyLeaves(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return leaveRequestRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId, pageable);
    }

    public Page<LeaveRequest> getAllLeaves(String status, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        if (status != null && !status.isBlank()) {
            return leaveRequestRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status.toUpperCase(), pageable);
        }
        return leaveRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    public List<LeaveBalance> getMyBalances(Integer year) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<LeaveBalance> balances = leaveBalanceRepository.findByTenantIdAndUserIdAndYear(tenantId, userId, targetYear);
        if (balances.isEmpty()) {
            List<LeaveType> types = getLeaveTypes();
            for (LeaveType t : types) {
                LeaveBalance nb = LeaveBalance.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .leaveTypeId(t.getId())
                        .year(targetYear)
                        .total(t.getDefaultBalance() != null ? t.getDefaultBalance() : 15)
                        .used(0)
                        .remaining(t.getDefaultBalance() != null ? t.getDefaultBalance() : 15)
                        .build();
                leaveBalanceRepository.save(nb);
            }
            balances = leaveBalanceRepository.findByTenantIdAndUserIdAndYear(tenantId, userId, targetYear);
        }
        return balances;
    }

    public List<LeaveType> getLeaveTypes() {
        UUID tenantId = TenantContext.requireTenantId();
        List<LeaveType> types = leaveTypeRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
        if (types.isEmpty()) {
            types = seedDefaultLeaveTypes(tenantId);
        }
        return types;
    }

    private List<LeaveType> seedDefaultLeaveTypes(UUID tenantId) {
        List<LeaveType> defaults = List.of(
                LeaveType.builder()
                        .tenantId(tenantId)
                        .name("Annual Leave")
                        .description("Paid annual paid vacation leave")
                        .defaultBalance(15)
                        .carryForward(true)
                        .status("ACTIVE")
                        .build(),
                LeaveType.builder()
                        .tenantId(tenantId)
                        .name("Sick Leave")
                        .description("Medical and health recovery leave")
                        .defaultBalance(10)
                        .carryForward(false)
                        .status("ACTIVE")
                        .build(),
                LeaveType.builder()
                        .tenantId(tenantId)
                        .name("Casual Leave")
                        .description("Personal and short emergency leave")
                        .defaultBalance(5)
                        .carryForward(false)
                        .status("ACTIVE")
                        .build(),
                LeaveType.builder()
                        .tenantId(tenantId)
                        .name("Unpaid Leave")
                        .description("Authorized unpaid time off")
                        .defaultBalance(30)
                        .carryForward(false)
                        .status("ACTIVE")
                        .build()
        );
        return leaveTypeRepository.saveAll(defaults);
    }

    @Transactional
    public LeaveType createLeaveType(CreateLeaveTypeRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        LeaveType type = LeaveType.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .defaultBalance(request.getDefaultBalance() != null ? request.getDefaultBalance() : 15)
                .carryForward(request.getCarryForward() != null ? request.getCarryForward() : false)
                .status("ACTIVE")
                .build();
        return leaveTypeRepository.save(type);
    }
}

