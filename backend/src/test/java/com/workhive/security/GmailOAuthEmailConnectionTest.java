package com.workhive.security;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.common.util.CryptoUtils;
import com.workhive.event.repository.OutboxEventRepository;
import com.workhive.module.user.dto.EmailConnectionDtos.*;
import com.workhive.module.user.entity.EmailConnection;
import com.workhive.module.user.repository.EmailConnectionRepository;
import com.workhive.module.user.service.EmailConnectionService;
import com.workhive.module.user.service.EmailService;
import com.workhive.module.user.service.email.EmailProvider;
import com.workhive.module.user.service.email.GmailProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GmailOAuthEmailConnectionTest {

    @Mock private EmailConnectionRepository emailConnectionRepository;
    @Mock private GmailProvider gmailProvider;
    @Mock private JavaMailSender mailSender;
    @Mock private OutboxEventRepository outboxEventRepository;

    private EmailConnectionService emailConnectionService;
    private EmailService emailService;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID adminA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID adminB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        emailConnectionService = new EmailConnectionService(emailConnectionRepository, gmailProvider);
        emailService = new EmailService(mailSender, outboxEventRepository, emailConnectionService);
        TenantContext.setContext(adminA, tenantA, "TENANT_ADMIN");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void initiateGmailConnection_generatesCsrfStateAndAuthUrl() {
        when(gmailProvider.isConfigured()).thenReturn(true);
        when(emailConnectionRepository.findByTenantIdAndUserIdAndProvider(tenantA, adminA, "GMAIL"))
                .thenReturn(Optional.empty());
        when(emailConnectionRepository.save(any(EmailConnection.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(gmailProvider.getAuthorizationUrl(anyString(), anyString()))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?client_id=test&state=xyz");

        ConnectResponse response = emailConnectionService.initiateGmailConnection(tenantA, adminA);

        assertNotNull(response);
        assertNotNull(response.getAuthUrl());
        assertTrue(response.getAuthUrl().contains("https://accounts.google.com"));
        verify(emailConnectionRepository).save(argThat(conn ->
                conn.getTenantId().equals(tenantA) &&
                conn.getUserId().equals(adminA) &&
                conn.getOauthState() != null &&
                !conn.getOauthState().isBlank() &&
                "NOT_CONNECTED".equals(conn.getStatus())
        ));
    }

    @Test
    void initiateGmailConnection_whenNotConfigured_throwsBadRequest() {
        when(gmailProvider.isConfigured()).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                emailConnectionService.initiateGmailConnection(tenantA, adminA)
        );
        assertTrue(ex.getMessage().contains("Google OAuth 2.0 is not configured"));
    }

    @Test
    void handleGmailCallback_withInvalidState_throwsBadRequest() {
        when(emailConnectionRepository.findByOauthState("invalid-state")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                emailConnectionService.handleGmailCallback("auth-code-123", "invalid-state")
        );
    }

    @Test
    void handleGmailCallback_withValidState_exchangesCodeAndConnects() {
        String validState = "valid-csrf-token-123";
        EmailConnection pendingConn = EmailConnection.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantA)
                .userId(adminA)
                .provider("GMAIL")
                .oauthState(validState)
                .status("NOT_CONNECTED")
                .build();

        EmailConnection connectedConn = EmailConnection.builder()
                .id(pendingConn.getId())
                .tenantId(tenantA)
                .userId(adminA)
                .provider("GMAIL")
                .emailAddress("sahn61393@gmail.com")
                .accessTokenEnc(CryptoUtils.encrypt("ya29.access-token"))
                .refreshTokenEnc(CryptoUtils.encrypt("1//refresh-token"))
                .status("CONNECTED")
                .build();

        when(emailConnectionRepository.findByOauthState(validState)).thenReturn(Optional.of(pendingConn));
        when(gmailProvider.exchangeAuthorizationCode(anyString(), anyString(), any(EmailConnection.class)))
                .thenReturn(connectedConn);
        when(emailConnectionRepository.save(any(EmailConnection.class))).thenReturn(connectedConn);

        ConnectionStatusResponse response = emailConnectionService.handleGmailCallback("auth-code-xyz", validState);

        assertNotNull(response);
        assertEquals("CONNECTED", response.getStatus());
        assertEquals("sahn61393@gmail.com", response.getEmailAddress());
        verify(emailConnectionRepository).save(any(EmailConnection.class));
    }

    @Test
    void disconnectEmail_revokesAndClearsTokens() {
        EmailConnection activeConn = EmailConnection.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantA)
                .userId(adminA)
                .provider("GMAIL")
                .emailAddress("sahn61393@gmail.com")
                .accessTokenEnc(CryptoUtils.encrypt("token"))
                .refreshTokenEnc(CryptoUtils.encrypt("refresh"))
                .status("CONNECTED")
                .build();

        when(emailConnectionRepository.findByTenantIdAndUserIdAndProvider(tenantA, adminA, "GMAIL"))
                .thenReturn(Optional.of(activeConn));

        DisconnectResponse response = emailConnectionService.disconnectEmail(tenantA, adminA);

        assertNotNull(response);
        assertEquals("DISCONNECTED", response.getStatus());
        verify(gmailProvider).revokeAccess(activeConn);
        verify(emailConnectionRepository).save(argThat(conn ->
                conn.getAccessTokenEnc() == null &&
                conn.getRefreshTokenEnc() == null &&
                "DISCONNECTED".equals(conn.getStatus())
        ));
    }

    @Test
    void multiTenantIsolation_tenantBCannotAccessTenantAConnection() {
        // Tenant A has connected Gmail
        EmailConnection connA = EmailConnection.builder()
                .tenantId(tenantA)
                .userId(adminA)
                .provider("GMAIL")
                .emailAddress("adminA@gmail.com")
                .status("CONNECTED")
                .build();

        when(emailConnectionRepository.findByTenantIdAndUserIdAndProvider(tenantB, adminB, "GMAIL"))
                .thenReturn(Optional.empty());

        // Admin B in Tenant B checks status
        ConnectionStatusResponse statusB = emailConnectionService.getConnectionStatus(tenantB, adminB);

        assertEquals("NOT_CONNECTED", statusB.getStatus());
        assertNull(statusB.getEmailAddress());
    }

    @Test
    void tokenEncryption_encryptAndDecryptRoundTrip() {
        String originalToken = "ya29.a0AfH6SMD_sensitive_oauth_bearer_token_123456789";
        String encrypted = CryptoUtils.encrypt(originalToken);

        assertNotNull(encrypted);
        assertNotEquals(originalToken, encrypted);

        String decrypted = CryptoUtils.decrypt(encrypted);
        assertEquals(originalToken, decrypted);
    }

    @Test
    void emailService_whenOAuthConnected_usesGmailProvider() throws Exception {
        EmailConnection conn = EmailConnection.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantA)
                .userId(adminA)
                .provider("GMAIL")
                .emailAddress("sahn61393@gmail.com")
                .accessTokenEnc(CryptoUtils.encrypt("token"))
                .refreshTokenEnc(CryptoUtils.encrypt("refresh"))
                .status("CONNECTED")
                .build();

        when(emailConnectionRepository.findByTenantIdAndUserIdAndProvider(tenantA, adminA, "GMAIL"))
                .thenReturn(Optional.of(conn));
        when(gmailProvider.canSend(conn)).thenReturn(true);
        when(gmailProvider.sendEmail(eq(conn), eq("employee@test.com"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("msg-id-12345");

        String status = emailService.sendInvitationEmail(
                tenantA, "Sah Enterprise", "employee@test.com", "John Doe",
                "EMPLOYEE", "Engineering", "Backend", "invite-token-abc",
                Instant.now().plusSeconds(86400), UUID.randomUUID(), "sahn61393@gmail.com", "Nitin Sah"
        );

        assertEquals("EMAIL_SENT", status);
        verify(gmailProvider).sendEmail(eq(conn), eq("employee@test.com"), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void emailService_whenNoOAuth_fallsBackToSmtp() {
        when(emailConnectionRepository.findByTenantIdAndUserIdAndProvider(tenantA, adminA, "GMAIL"))
                .thenReturn(Optional.empty());

        // JavaMailSender is present (or null in test environment), falls back cleanly
        String status = emailService.sendInvitationEmail(
                tenantA, "Sah Enterprise", "employee@test.com", "John Doe",
                "EMPLOYEE", "Engineering", "Backend", "invite-token-abc",
                Instant.now().plusSeconds(86400), UUID.randomUUID(), "sahn61393@gmail.com", "Nitin Sah"
        );

        // When mailSender is mocked/null, status reflects clean fallback without throwing unhandled exception
        assertNotNull(status);
    }

    @Test
    void oauthSendPath_selectedWhenConnected_noSmtpPasswordRequired() throws Exception {
        // HR Admin (monsterridersah@gmail.com) has authorized Gmail OAuth
        EmailConnection conn = EmailConnection.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantA)
                .userId(adminA)
                .provider("GMAIL")
                .emailAddress("monsterridersah@gmail.com")
                .accessTokenEnc(CryptoUtils.encrypt("ya29.live-oauth-token"))
                .refreshTokenEnc(CryptoUtils.encrypt("1//live-refresh-token"))
                .status("CONNECTED")
                .build();

        when(emailConnectionRepository.findByTenantIdAndUserIdAndProvider(tenantA, adminA, "GMAIL"))
                .thenReturn(Optional.of(conn));
        when(gmailProvider.canSend(conn)).thenReturn(true);
        when(gmailProvider.sendEmail(eq(conn), eq("ankir2201@gmail.com"), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("gmail-rest-msg-99999");

        // Send invitation to ankir2201@gmail.com
        String status = emailService.sendInvitationEmail(
                tenantA, "WorkHive", "ankir2201@gmail.com", "Ankit Sharma",
                "EMPLOYEE", "Engineering", "Platform", "invite-token-xyz",
                Instant.now().plusSeconds(86400), UUID.randomUUID(), "monsterridersah@gmail.com", "HR Admin"
        );

        // Verification 1: OAuth path selected and succeeded
        assertEquals("EMAIL_SENT", status);
        verify(gmailProvider).sendEmail(eq(conn), eq("ankir2201@gmail.com"), anyString(), anyString(), anyString(), anyString());

        // Verification 2: SMTP was NOT called, zero SMTP App Password required
        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void crossTenantIsolation_cannotUseAnotherTenantOAuthConnection() {
        // Tenant A admin (monsterridersah@gmail.com) has OAuth connected
        EmailConnection connA = EmailConnection.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantA)
                .userId(adminA)
                .provider("GMAIL")
                .emailAddress("monsterridersah@gmail.com")
                .status("CONNECTED")
                .build();

        when(emailConnectionRepository.findByTenantIdAndUserIdAndProvider(tenantB, adminB, "GMAIL"))
                .thenReturn(Optional.empty());
        when(emailConnectionRepository.findByTenantIdAndEmailAddressAndProvider(tenantB, "monsterridersah@gmail.com", "GMAIL"))
                .thenReturn(Optional.empty());
        when(emailConnectionRepository.findByTenantIdAndProvider(tenantB, "GMAIL"))
                .thenReturn(Optional.empty());

        // Tenant B attempts to resolve email connection using Tenant A's sender email
        EmailConnection resolvedForTenantB = emailConnectionService.getConnection(tenantB, adminB, "monsterridersah@gmail.com");

        // Verification: Tenant B strictly receives null, Tenant A's connection is NOT leaked
        assertNull(resolvedForTenantB);
    }
}
