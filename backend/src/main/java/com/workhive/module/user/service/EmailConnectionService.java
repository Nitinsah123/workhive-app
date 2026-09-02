package com.workhive.module.user.service;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.user.dto.EmailConnectionDtos.*;
import com.workhive.module.user.entity.EmailConnection;
import com.workhive.module.user.repository.EmailConnectionRepository;
import com.workhive.module.user.service.email.EmailProvider;
import com.workhive.module.user.service.email.GmailProvider;
import com.workhive.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailConnectionService {

    private static final Logger log = LoggerFactory.getLogger(EmailConnectionService.class);

    private final EmailConnectionRepository emailConnectionRepository;
    private final GmailProvider gmailProvider;

    @Value("${google.oauth.redirect-uri:http://localhost:8080/api/email-connections/gmail/callback}")
    private String gmailRedirectUri = "http://localhost:8080/api/email-connections/gmail/callback";

    public EmailConnectionService(EmailConnectionRepository emailConnectionRepository,
                                  GmailProvider gmailProvider) {
        this.emailConnectionRepository = emailConnectionRepository;
        this.gmailProvider = gmailProvider;
    }

    /**
     * Get the email connection status for the current admin.
     */
    public ConnectionStatusResponse getConnectionStatus(UUID tenantId, UUID userId) {
        Optional<EmailConnection> connOpt = emailConnectionRepository
                .findByTenantIdAndUserIdAndProvider(tenantId, userId, "GMAIL");

        if (connOpt.isEmpty()) {
            return ConnectionStatusResponse.builder()
                    .provider("GMAIL")
                    .status("NOT_CONNECTED")
                    .build();
        }

        EmailConnection conn = connOpt.get();
        return ConnectionStatusResponse.builder()
                .provider(conn.getProvider())
                .emailAddress(conn.getEmailAddress())
                .status(conn.getStatus())
                .lastSendAt(conn.getLastSendAt())
                .errorMessage(conn.getErrorMessage())
                .connectedAt(conn.getCreatedAt())
                .build();
    }

    /**
     * Initiate Gmail OAuth connection flow.
     * Generates CSRF state and returns Google authorization URL.
     */
    @Transactional
    public ConnectResponse initiateGmailConnection(UUID tenantId, UUID userId) {
        if (!gmailProvider.isConfigured()) {
            throw new BadRequestException("Google OAuth 2.0 is not configured on the server. Please configure GOOGLE_OAUTH_CLIENT_ID and GOOGLE_OAUTH_CLIENT_SECRET in the environment.");
        }

        // Generate cryptographically secure state
        byte[] stateBytes = new byte[32];
        new SecureRandom().nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);

        // Find or create the EmailConnection record
        EmailConnection conn = emailConnectionRepository
                .findByTenantIdAndUserIdAndProvider(tenantId, userId, "GMAIL")
                .orElse(EmailConnection.builder()
                        .tenantId(tenantId)
                        .userId(userId)
                        .provider("GMAIL")
                        .status("NOT_CONNECTED")
                        .build());

        conn.setOauthState(state);
        conn.setStatus("NOT_CONNECTED");
        conn.setErrorMessage(null);
        emailConnectionRepository.save(conn);

        String authUrl = gmailProvider.getAuthorizationUrl(state, gmailRedirectUri);

        log.info("🔗 Gmail OAuth initiated for tenant=[{}] user=[{}]", tenantId, userId);
        return new ConnectResponse(authUrl, "Redirect to Google to authorize your Gmail account.");
    }

    /**
     * Handle the Gmail OAuth callback.
     * Validates CSRF state, exchanges code for tokens, stores encrypted credentials.
     */
    @Transactional
    public ConnectionStatusResponse handleGmailCallback(String code, String state) {
        if (state == null || state.isBlank()) {
            throw new BadRequestException("Missing OAuth state parameter");
        }

        // Find the connection by state (CSRF validation)
        EmailConnection conn = emailConnectionRepository.findByOauthState(state)
                .orElseThrow(() -> new BadRequestException("Invalid or expired OAuth state. Please try connecting again."));

        // Exchange the authorization code for tokens
        conn = gmailProvider.exchangeAuthorizationCode(code, gmailRedirectUri, conn);
        emailConnectionRepository.save(conn);

        log.info("✅ Gmail OAuth callback processed for tenant=[{}] email=[{}] status=[{}]",
                conn.getTenantId(), conn.getEmailAddress(), conn.getStatus());

        return ConnectionStatusResponse.builder()
                .provider(conn.getProvider())
                .emailAddress(conn.getEmailAddress())
                .status(conn.getStatus())
                .connectedAt(conn.getCreatedAt())
                .errorMessage(conn.getErrorMessage())
                .build();
    }

    /**
     * Disconnect the Gmail email connection, revoking OAuth access.
     */
    @Transactional
    public DisconnectResponse disconnectEmail(UUID tenantId, UUID userId) {
        EmailConnection conn = emailConnectionRepository
                .findByTenantIdAndUserIdAndProvider(tenantId, userId, "GMAIL")
                .orElseThrow(() -> new ResourceNotFoundException("No Gmail connection found"));

        // Revoke the OAuth token
        gmailProvider.revokeAccess(conn);

        // Clear sensitive data
        conn.setAccessTokenEnc(null);
        conn.setRefreshTokenEnc(null);
        conn.setTokenExpiresAt(null);
        conn.setStatus("DISCONNECTED");
        conn.setErrorMessage(null);
        conn.setOauthState(null);
        emailConnectionRepository.save(conn);

        log.info("🔓 Gmail disconnected for tenant=[{}] user=[{}]", tenantId, userId);
        return new DisconnectResponse("Gmail email connection disconnected.", "DISCONNECTED");
    }

    /**
     * Get the email provider (GmailProvider) for a connected admin.
     * Returns null if no connection exists or not connected.
     */
    public EmailProvider getProviderForUser(UUID tenantId, UUID userId) {
        EmailConnection conn = getConnection(tenantId, userId);
        if (conn != null && gmailProvider.canSend(conn)) {
            return gmailProvider;
        }
        return null;
    }

    /**
     * Get the email provider for an existing connection.
     */
    public EmailProvider getProviderForConnection(EmailConnection conn) {
        if (conn != null && "GMAIL".equalsIgnoreCase(conn.getProvider()) && gmailProvider.canSend(conn)) {
            return gmailProvider;
        }
        return null;
    }

    /**
     * Get the EmailConnection for a connected admin by tenantId and userId.
     * Returns null if not connected.
     */
    public EmailConnection getConnection(UUID tenantId, UUID userId) {
        return getConnection(tenantId, userId, null);
    }

    /**
     * Get the EmailConnection resolving by tenantId, userId, or senderEmail.
     * Returns null if not connected.
     */
    public EmailConnection getConnection(UUID tenantId, UUID userId, String senderEmail) {
        if (tenantId == null) {
            return null;
        }
        if (userId != null) {
            Optional<EmailConnection> conn = emailConnectionRepository
                    .findByTenantIdAndUserIdAndProvider(tenantId, userId, "GMAIL")
                    .filter(c -> "CONNECTED".equals(c.getStatus()));
            if (conn.isPresent()) return conn.get();
        }
        if (senderEmail != null && !senderEmail.isBlank()) {
            Optional<EmailConnection> conn = emailConnectionRepository
                    .findByTenantIdAndEmailAddressAndProvider(tenantId, senderEmail.toLowerCase().trim(), "GMAIL")
                    .filter(c -> "CONNECTED".equals(c.getStatus()));
            if (conn.isPresent()) return conn.get();
        }
        Optional<EmailConnection> conn = emailConnectionRepository
                .findByTenantIdAndProvider(tenantId, "GMAIL")
                .filter(c -> "CONNECTED".equals(c.getStatus()));
        if (conn.isPresent()) return conn.get();

        return null;
    }

    /**
     * Save / update an email connection (e.g. after OAuth token refresh).
     */
    @Transactional
    public EmailConnection saveConnection(EmailConnection connection) {
        return emailConnectionRepository.save(connection);
    }

    /**
     * Update last send timestamp after successful email delivery.
     */
    @Transactional
    public void markLastSend(UUID connectionId) {
        emailConnectionRepository.findById(connectionId).ifPresent(conn -> {
            conn.setLastSendAt(Instant.now());
            emailConnectionRepository.save(conn);
        });
    }

    /**
     * Mark a connection as requiring re-authorization (e.g., after token refresh failure).
     */
    @Transactional
    public void markReauthRequired(UUID connectionId, String errorMessage) {
        emailConnectionRepository.findById(connectionId).ifPresent(conn -> {
            conn.setStatus("REAUTH_REQUIRED");
            conn.setErrorMessage(errorMessage);
            emailConnectionRepository.save(conn);
        });
    }
}
