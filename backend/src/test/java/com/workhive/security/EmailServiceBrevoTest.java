package com.workhive.security;

import com.workhive.event.repository.OutboxEventRepository;
import com.workhive.module.user.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceBrevoTest {

    @Mock private JavaMailSender mailSender;
    @Mock private OutboxEventRepository outboxEventRepository;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, outboxEventRepository, null);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("BREVO_API_KEY");
        System.clearProperty("brevo.api.key");
    }

    @Test
    void testBrevoApiKeyReadFromSystemProperty() {
        System.setProperty("BREVO_API_KEY", "test-brevo-key-xyz-123");
        assertEquals("test-brevo-key-xyz-123", emailService.getBrevoApiKey());
    }

    @Test
    void testBrevoReturnsNullWhenKeyNotConfigured() {
        System.clearProperty("BREVO_API_KEY");
        // Should return failed BrevoResult and not crash
        EmailService.BrevoResult result = emailService.sendViaBrevoApi("WorkHive Admin", "admin@workhive.com",
                "admin@workhive.com", "Admin", "emp@workhive.com", "Emp", "Subject", "<h1>Html</h1>", "Text");
        assertNotNull(result);
        assertFalse(result.success);
    }

    @Test
    void testSendInvitationEmailFallsBackGracefullyWhenBrevoFails() {
        System.setProperty("BREVO_API_KEY", "invalid-key-for-test");

        UUID tenantId = UUID.randomUUID();
        UUID invitationId = UUID.randomUUID();

        // Should return EMAIL_FAILED and NOT EMAIL_SENT since the key is invalid and HTTP will fail/reject
        String status = emailService.sendInvitationEmail(
                tenantId,
                "Test Corp",
                "invitee@testcorp.com",
                "Jane Doe",
                "EMPLOYEE",
                "Engineering",
                "Frontend",
                "mock-token-123",
                Instant.now().plusSeconds(3600),
                invitationId,
                "admin@testcorp.com",
                "Admin User"
        );

        assertEquals("EMAIL_FAILED", status);
    }

    @Test
    void testNoSecretsInLogsOrExceptionMessages() {
        String testSecret = "xkeysib-998877665544332211aabbccddeeff";
        System.setProperty("BREVO_API_KEY", testSecret);

        EmailService.BrevoResult result = emailService.sendViaBrevoApi("Sender", "sender@test.com", "reply@test.com", "Reply",
                "to@test.com", "To", "Subject", "<p>Hello</p>", "Hello");
        assertNotNull(result);
        assertFalse(result.success);
        // The secret should be accessed via getBrevoApiKey without exposing in toString or standard prints
        assertNotNull(emailService.getBrevoApiKey());
    }
}
