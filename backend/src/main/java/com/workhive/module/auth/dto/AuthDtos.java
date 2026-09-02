package com.workhive.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDtos {

    @Data
    public static class CreateWorkspaceRequest {
        // Organization
        @NotBlank(message = "Company name is required")
        private String companyName;

        @NotBlank(message = "Company code is required")
        @Size(min = 2, max = 10, message = "Company code must be 2-10 characters")
        private String companyCode;

        private String industry;
        private String timezone;
        private String workingDays;
        private String logoUrl;

        // Admin
        @NotBlank(message = "Admin full name is required")
        private String adminFullName;

        @NotBlank(message = "Admin email is required")
        @Email(message = "Invalid email format")
        private String adminEmail;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String adminPassword;

        private String adminPhone;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private UserInfo user;
        private TenantInfo tenant;

        @Data
        public static class UserInfo {
            private String id;
            private String email;
            private String fullName;
            private String role;
            private String employeeCode;
            private String avatarUrl;
            private String departmentId;
            private String teamId;
            private String managerId;
        }

        @Data
        public static class TenantInfo {
            private String id;
            private String name;
            private String code;
            private String logoUrl;
            private String timezone;
        }
    }
}
