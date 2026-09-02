package com.workhive.module.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

public class AnnouncementDtos {

    @Data
    public static class CreateAnnouncementRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Content is required")
        private String content;

        private String targetType = "ORGANIZATION";
        private UUID targetId;
        private String priority = "NORMAL";
        private Boolean published = true;
    }
}
