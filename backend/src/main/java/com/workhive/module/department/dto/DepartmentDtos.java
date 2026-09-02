package com.workhive.module.department.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

public class DepartmentDtos {

    @Data
    public static class CreateDepartmentRequest {
        @NotBlank(message = "Department name is required")
        private String name;
        private String description;
        private UUID managerId;
    }

    @Data
    public static class UpdateDepartmentRequest {
        @NotBlank(message = "Department name is required")
        private String name;
        private String description;
        private UUID managerId;
        private String status;
    }
}
