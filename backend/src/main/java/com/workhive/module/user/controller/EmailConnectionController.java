package com.workhive.module.user.controller;

import com.workhive.module.user.dto.EmailConnectionDtos.*;
import com.workhive.module.user.service.EmailConnectionService;
import com.workhive.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/email-connections")
public class EmailConnectionController {

    private static final Logger log = LoggerFactory.getLogger(EmailConnectionController.class);

    private final EmailConnectionService emailConnectionService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public EmailConnectionController(EmailConnectionService emailConnectionService) {
        this.emailConnectionService = emailConnectionService;
    }

    /**
     * GET /api/email-connections/status
     * Returns the current admin's email connection status.
     */
    @GetMapping("/status")
    public ResponseEntity<ConnectionStatusResponse> getStatus() {
        var tenantId = TenantContext.getTenantId();
        var userId = TenantContext.getUserId();
        return ResponseEntity.ok(emailConnectionService.getConnectionStatus(tenantId, userId));
    }

    /**
     * POST /api/email-connections/gmail/connect
     * Initiates Gmail OAuth flow. Returns the Google authorization URL.
     */
    @PostMapping("/gmail/connect")
    public ResponseEntity<ConnectResponse> connectGmail() {
        var tenantId = TenantContext.getTenantId();
        var userId = TenantContext.getUserId();
        return ResponseEntity.ok(emailConnectionService.initiateGmailConnection(tenantId, userId));
    }

    /**
     * GET /api/email-connections/gmail/callback
     * Public endpoint — OAuth callback from Google.
     * Validates state, exchanges code, redirects to frontend settings.
     */
    @GetMapping("/gmail/callback")
    public ResponseEntity<Void> gmailCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error) {

        if (error != null) {
            log.warn("Gmail OAuth callback error: {}", error);
            String redirectUrl = frontendUrl.replaceAll("/+$", "") + "/settings?email_error=" + error;
            return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
        }

        try {
            ConnectionStatusResponse result = emailConnectionService.handleGmailCallback(code, state);
            String redirectUrl = frontendUrl.replaceAll("/+$", "")
                    + "/settings?email_connected=true&email=" + result.getEmailAddress();
            return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
        } catch (Exception e) {
            log.error("Gmail OAuth callback processing failed: {}", e.getMessage());
            String redirectUrl = frontendUrl.replaceAll("/+$", "") + "/settings?email_error=callback_failed";
            return ResponseEntity.status(302).location(URI.create(redirectUrl)).build();
        }
    }

    /**
     * POST /api/email-connections/gmail/callback-exchange
     * Alternative: Frontend-initiated code exchange (for popup/SPA flow).
     */
    @PostMapping("/gmail/callback-exchange")
    public ResponseEntity<ConnectionStatusResponse> callbackExchange(@RequestBody CallbackRequest request) {
        ConnectionStatusResponse result = emailConnectionService.handleGmailCallback(request.getCode(), request.getState());
        return ResponseEntity.ok(result);
    }

    /**
     * DELETE /api/email-connections/disconnect
     * Disconnects the current admin's Gmail email connection.
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<DisconnectResponse> disconnect() {
        var tenantId = TenantContext.getTenantId();
        var userId = TenantContext.getUserId();
        return ResponseEntity.ok(emailConnectionService.disconnectEmail(tenantId, userId));
    }

    /**
     * POST /api/email-connections/gmail/reconnect
     * Re-initiates the Gmail OAuth flow (for re-authorization).
     */
    @PostMapping("/gmail/reconnect")
    public ResponseEntity<ConnectResponse> reconnect() {
        var tenantId = TenantContext.getTenantId();
        var userId = TenantContext.getUserId();
        return ResponseEntity.ok(emailConnectionService.initiateGmailConnection(tenantId, userId));
    }
}
