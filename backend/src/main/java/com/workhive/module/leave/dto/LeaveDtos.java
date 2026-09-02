package com.workhive.module.leave.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

public class LeaveDtos {

    @Data
    public static class ApplyLeaveRequest {
        @NotNull(message = "Leave type is required")
        private UUID leaveTypeId;

        @NotNull(message = "Start date is required")
        private LocalDate startDate;

        @NotNull(message = "End date is required")
        private LocalDate endDate;

        private String reason;
        private UUID supportingDocId;
    }

    @Data
    public static class ReviewLeaveRequest {
        @NotNull(message = "Status is required")
        private String status; // APPROVED or REJECTED

        private String reviewComment;
    }

    @Data
    public static class CreateLeaveTypeRequest {
        @NotNull(message = "Name is required")
        private String name;
        private String description;
        private Integer defaultBalance = 15;
        private Boolean carryForward = false;
    }
}
