package com.workhive.module.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

public class UserDtos {

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        private String role = "EMPLOYEE";
        private UUID departmentId;
        private UUID teamId;
        private UUID managerId;
        private String phone;
    }

    @Data
    public static class UpdateUserRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;
        private String role;
        private UUID departmentId;
        private UUID teamId;
        private UUID managerId;
        private String phone;
        private String avatarUrl;
        private String timezone;
        private String status;
    }

    @Data
    public static class InviteUserRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        private String name;
        private String role = "EMPLOYEE";
        private UUID departmentId;
        private UUID teamId;
        private UUID managerId;
    }

    @Data
    public static class InviteUserResponse {
        private UUID id;
        private String email;
        private String name;
        private String role;
        private String token;
        private String inviteUrl;
        private String emailStatus; // EMAIL_SENT, EMAIL_FAILED, EMAIL_PENDING
        private String message;
        private String errorMessage;
        private UUID departmentId;
        private UUID teamId;
        private UUID managerId;
    }

    @Data
    public static class InvitationDetailsResponse {
        private UUID id;
        private String email;
        private String name;
        private String role;
        private String tenantName;
        private String tenantCode;
        private String departmentName;
        private String teamName;
        private String managerName;
        private String status;
        private boolean valid;
        private String message;
    }

    @Data
    public static class AcceptInvitationRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        private String phone;
    }

    @Data
    public static class UpdateProfileRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;
        private String phone;
        private String avatarUrl;
        private String timezone;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        private String newPassword;
    }
}
