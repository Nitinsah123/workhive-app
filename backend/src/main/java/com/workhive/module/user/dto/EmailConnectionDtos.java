package com.workhive.module.user.dto;

import lombok.*;
import java.time.Instant;

public class EmailConnectionDtos {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ConnectionStatusResponse {
        private String provider;
        private String emailAddress;
        private String status; // NOT_CONNECTED, CONNECTED, ERROR, REAUTH_REQUIRED, DISCONNECTED
        private Instant lastSendAt;
        private String errorMessage;
        private Instant connectedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ConnectResponse {
        private String authUrl;
        private String message;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CallbackRequest {
        private String code;
        private String state;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DisconnectResponse {
        private String message;
        private String status;
    }
}
