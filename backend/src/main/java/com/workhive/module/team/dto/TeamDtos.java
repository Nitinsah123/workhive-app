package com.workhive.module.team.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

public class TeamDtos {

    @Data
    public static class CreateTeamRequest {
        @NotBlank(message = "Team name is required")
        private String name;
        private UUID departmentId;
        private UUID leadId;
        private String description;
    }

    @Data
    public static class UpdateTeamRequest {
        @NotBlank(message = "Team name is required")
        private String name;
        private UUID departmentId;
        private UUID leadId;
        private String description;
        private String status;
    }
}
