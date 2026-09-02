package com.workhive.security;

import com.workhive.event.repository.OutboxEventRepository;
import com.workhive.module.user.entity.EmailConnection;
import com.workhive.module.user.service.EmailConnectionService;
import com.workhive.module.user.service.EmailService;
import com.workhive.module.user.service.email.EmailProvider;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.time.Instant;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceDeliveryTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private EmailConnectionService emailConnectionService;
    @Mock private JavaMailSender javaMailSender;
    @Mock private EmailProvider emailProvider;

    private EmailService emailService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();
    private final String adminEmail = "monsterridersah@gmail.com";
    private final String adminName = "Monster Rider";
    private final String recipientEmail = "ankir2201@gmail.com";
    private final String recipientName = "Ankir Employee";
    private final String token = "secure-invitation-token-12345";
    private final Instant expiresAt = Instant.now().plusSeconds(86400 * 7);

    @BeforeEach
    void setUp() {
        emailService = new EmailService(javaMailSender, outboxEventRepository, emailConnectionService);
    }

    @Test
    @DisplayName("1. Successful Provider Send - Gmail OAuth provider returns EMAIL_SENT")
    void testSendInvitation_GmailOAuth_SuccessfulProviderSend() throws Exception {
        EmailConnection conn = EmailConnection.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .userId(adminUserId)
                .emailAddress(adminEmail)
                .provider("GMAIL")
                .status("CONNECTED")
                .build();

        when(emailConnectionService.getConnection(eq(tenantId), any(), eq(adminEmail))).thenReturn(conn);
        when(emailConnectionService.getProviderForConnection(conn)).thenReturn(emailProvider);
        when(emailProvider.canSend(conn)).thenReturn(true);
        when(emailProvider.sendEmail(eq(conn), eq(recipientEmail), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("gmail-message-id-9988");

        String status = emailService.sendInvitationEmail(
                tenantId,
                "Apex Cloud Systems",
                recipientEmail,
                recipientName,
                "EMPLOYEE",
                "Engineering",
                "Platform",
                token,
                expiresAt,
                UUID.randomUUID(),
                adminEmail,
                adminName
        );

        assertEquals("EMAIL_SENT", status);
        verify(emailProvider).sendEmail(eq(conn), eq(recipientEmail), anyString(), anyString(), anyString(), anyString());
        verify(emailConnectionService).markLastSend(conn.getId());
    }

    @Test
    @DisplayName("2. Exact Recipient & Sender Resolution - Live SMTP send sets exact recipient and dynamic sender")
    void testSendInvitation_ExactRecipientAndSenderResolution() {
        // Setup live JavaMailSenderImpl with mock session
        JavaMailSenderImpl realMailSender = spy(new JavaMailSenderImpl());
        realMailSender.setHost("smtp.gmail.com");
        realMailSender.setPort(587);
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        realMailSender.setJavaMailProperties(props);

        EmailService liveEmailService = new EmailService(realMailSender, outboxEventRepository, emailConnectionService);
        when(emailConnectionService.getConnection(eq(tenantId), any(), eq(adminEmail))).thenReturn(null);

        // Mock send to succeed
        doNothing().when(realMailSender).send(any(MimeMessage.class));

        String status = liveEmailService.sendInvitationEmail(
                tenantId,
                "Apex Cloud Systems",
                "   Ankir2201@GMAIL.COM   ", // Raw input with spaces and mixed case
                recipientName,
                "EMPLOYEE",
                "Engineering",
                "Platform",
                token,
                expiresAt,
                UUID.randomUUID(),
                adminEmail,
                adminName
        );

        assertEquals("EMAIL_SENT", status);

        // Verify exact recipient was sanitized to lowercase without spaces
        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(realMailSender).send(messageCaptor.capture());
        MimeMessage sentMsg = messageCaptor.getValue();
        assertNotNull(sentMsg);

        // Verify dynamic SMTP username was bound to inviting admin's email
        assertEquals(adminEmail, realMailSender.getUsername());
    }

    @Test
    @DisplayName("3. Failure State - Real SMTP transmission error reports EMAIL_FAILED (no fake success)")
    void testSendInvitation_SmtpFailure_ReportsEmailFailed() {
        JavaMailSenderImpl realMailSender = spy(new JavaMailSenderImpl());
        realMailSender.setHost("smtp.gmail.com");
        realMailSender.setPort(587);

        EmailService liveEmailService = new EmailService(realMailSender, outboxEventRepository, emailConnectionService);
        when(emailConnectionService.getConnection(eq(tenantId), any(), eq(adminEmail))).thenReturn(null);

        // Simulate SMTP Authentication Required error from Google
        doThrow(new MailSendException("530 5.7.0 Authentication Required")).when(realMailSender).send(any(MimeMessage.class));

        String status = liveEmailService.sendInvitationEmail(
                tenantId,
                "Apex Cloud Systems",
                recipientEmail,
                recipientName,
                "EMPLOYEE",
                "Engineering",
                "Platform",
                token,
                expiresAt,
                UUID.randomUUID(),
                adminEmail,
                adminName
        );

        assertEquals("EMAIL_FAILED", status, "Must report EMAIL_FAILED upon provider error, never fake EMAIL_SENT");
    }

    @Test
    @DisplayName("4. Truthful Status - Local MailHog mock capture reports EMAIL_FAILED for real recipient delivery")
    void testSendInvitation_LocalMailHog_ReportsEmailFailedForRealDelivery() {
        JavaMailSenderImpl mockMailSender = spy(new JavaMailSenderImpl());
        mockMailSender.setHost("localhost");
        mockMailSender.setPort(1025);

        // Service configured with localhost
        EmailService localEmailService = new EmailService(mockMailSender, outboxEventRepository, emailConnectionService);
        when(emailConnectionService.getConnection(eq(tenantId), any(), eq(adminEmail))).thenReturn(null);
        doNothing().when(mockMailSender).send(any(MimeMessage.class));

        String status = localEmailService.sendInvitationEmail(
                tenantId,
                "Apex Cloud Systems",
                recipientEmail,
                recipientName,
                "EMPLOYEE",
                null,
                null,
                token,
                expiresAt,
                UUID.randomUUID(),
                adminEmail,
                adminName
        );

        // Local MailHog does not deliver to external recipient inbox -> must not claim EMAIL_SENT!
        assertEquals("EMAIL_FAILED", status);
    }

    @Test
    @DisplayName("5. Central Notifications Sender - From central address, Reply-To is logged-in Admin")
    void testSendInvitation_CentralNotificationsSmtp_DispatchesWithAdminReplyTo() throws Exception {
        JavaMailSenderImpl realMailSender = spy(new JavaMailSenderImpl());
        realMailSender.setHost("smtp.gmail.com");
        realMailSender.setPort(587);

        String centralNotificationsEmail = "workhivenotifications@gmail.com";
        System.setProperty("SPRING_MAIL_USERNAME", centralNotificationsEmail);
        System.setProperty("SPRING_MAIL_PASSWORD", "test-app-password");

        try {
            String adminTestEmail = "ankitsag746@gmail.com";
            String adminTestName = "Ankit Sagar";
            String employeeRecipient = "k609558821@gmail.com";

            EmailService liveEmailService = new EmailService(realMailSender, outboxEventRepository, emailConnectionService);
            when(emailConnectionService.getConnection(eq(tenantId), any(), eq(adminTestEmail))).thenReturn(null);
            doNothing().when(realMailSender).send(any(MimeMessage.class));

            String status = liveEmailService.sendInvitationEmail(
                    tenantId,
                    "Acme Enterprise",
                    employeeRecipient,
                    "Test Employee",
                    "EMPLOYEE",
                    "Engineering",
                    "Core Platform",
                    token,
                    expiresAt,
                    UUID.randomUUID(),
                    adminTestEmail,
                    adminTestName
            );

            assertEquals("EMAIL_SENT", status);
            assertEquals(centralNotificationsEmail, realMailSender.getUsername());
            assertEquals("test-app-password", realMailSender.getPassword());

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            verify(realMailSender).send(captor.capture());
            MimeMessage sent = captor.getValue();
            assertNotNull(sent);

            // Verify From address is the central notifications email
            assertEquals(1, sent.getFrom().length);
            assertTrue(sent.getFrom()[0].toString().contains(centralNotificationsEmail));

            // Verify Reply-To address is the logged-in Admin
            assertNotNull(sent.getReplyTo());
            assertTrue(sent.getReplyTo().length > 0);
            assertTrue(sent.getReplyTo()[0].toString().contains(adminTestEmail));
        } finally {
            System.clearProperty("SPRING_MAIL_USERNAME");
            System.clearProperty("SPRING_MAIL_PASSWORD");
        }
    }
}
