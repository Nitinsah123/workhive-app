package com.workhive.module.user.service;

import com.workhive.event.entity.OutboxEvent;
import com.workhive.event.repository.OutboxEventRepository;
import com.workhive.module.user.entity.EmailConnection;
import com.workhive.module.user.service.email.EmailProvider;
import com.workhive.module.user.service.email.GmailProvider;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final OutboxEventRepository outboxEventRepository;
    private final EmailConnectionService emailConnectionService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl = "http://localhost:3000";

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost = "smtp.gmail.com";

    @Value("${spring.mail.port:587}")
    private int mailPort = 587;

    @Value("${spring.mail.username:}")
    private String fromAddress = "";

    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                        OutboxEventRepository outboxEventRepository,
                        @Autowired(required = false) EmailConnectionService emailConnectionService) {
        this.mailSender = mailSender;
        this.outboxEventRepository = outboxEventRepository;
        this.emailConnectionService = emailConnectionService;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    /**
     * Dispatches an employee workspace invitation email.
     * Guaranteed recipient: exact email address entered by the Admin.
     * Sender identity: Authenticated Tenant Admin / Workspace Sender.
     *
     * @return "EMAIL_SENT" or "EMAIL_FAILED"
     */
    public String sendInvitationEmail(UUID tenantId,
                                      String tenantName,
                                      String recipientEmail,
                                      String employeeName,
                                      String role,
                                      String departmentName,
                                      String teamName,
                                      String inviteToken,
                                      Instant expiresAt,
                                      UUID invitationId,
                                      String senderEmail,
                                      String senderName) {
        return sendInvitationEmail(tenantId, tenantName, recipientEmail, employeeName, role,
                departmentName, teamName, inviteToken, expiresAt, invitationId, senderEmail, senderName, null);
    }

    public String sendInvitationEmail(UUID tenantId,
                                      String tenantName,
                                      String recipientEmail,
                                      String employeeName,
                                      String role,
                                      String departmentName,
                                      String teamName,
                                      String inviteToken,
                                      Instant expiresAt,
                                      UUID invitationId,
                                      String senderEmail,
                                      String senderName,
                                      UUID senderUserId) {

        String cleanEmail = recipientEmail.toLowerCase().trim();
        String displayName = (employeeName != null && !employeeName.isBlank()) ? employeeName.trim() : "Team Member";
        String inviteUrl = frontendUrl.replaceAll("/+$", "") + "/accept-invitation?token=" + inviteToken;

        String formattedExpiry = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a z")
                .withZone(ZoneId.of("UTC"))
                .format(expiresAt != null ? expiresAt : Instant.now().plusSeconds(7 * 24 * 3600));

        String subject = "You've been invited to join " + tenantName + " on WorkHive";

        String senderDisplay = (senderName != null && !senderName.isBlank()) ? senderName : "Workspace Administrator";
        String centralSender = getCentralMailUsername();
        String effectiveFrom = (centralSender != null && !centralSender.isBlank() && !centralSender.contains("noreply"))
                ? centralSender
                : (senderEmail != null && !senderEmail.isBlank() ? senderEmail : "noreply@workhive.internal");

        String plainTextBody = "Hello " + displayName + ",\n\n" +
                "You have been invited by " + senderDisplay + " (" + (senderEmail != null ? senderEmail : "Admin") + ") to join " + tenantName + " on WorkHive.\n\n" +
                "Organization: " + tenantName + "\n" +
                "Role: " + (role != null ? role : "Employee") + "\n" +
                (departmentName != null ? "Department: " + departmentName + "\n" : "") +
                (teamName != null ? "Team: " + teamName + "\n" : "") +
                "\nPlease click the link below to accept your invitation and set up your account password:\n" +
                inviteUrl + "\n\n" +
                "⏰ This invitation will expire on: " + formattedExpiry + ".\n\n" +
                "If you were not expecting this invitation, you can safely ignore this email.\n\n" +
                "Best regards,\n" +
                senderDisplay + "\n" +
                tenantName + " via WorkHive";

        String htmlBody = buildInvitationHtml(tenantName, displayName, role, departmentName, teamName, inviteUrl, formattedExpiry, senderEmail, senderName);

        String emailStatus = "EMAIL_PENDING";
        String errorMessage = null;

        // 1. Try per-admin Gmail OAuth provider first
        boolean oauthSuccess = false;
        if (emailConnectionService != null) {
            try {
                // Look up the admin's OAuth email connection by tenant, admin user ID, or sender email
                UUID adminUserId = (senderUserId != null) ? senderUserId : findAdminUserId(tenantId, senderEmail);
                EmailConnection conn = emailConnectionService.getConnection(tenantId, adminUserId, senderEmail);
                if (conn != null) {
                    EmailProvider provider = emailConnectionService.getProviderForConnection(conn);
                    if (provider != null && provider.canSend(conn)) {
                        String msgId = provider.sendEmail(conn, cleanEmail, subject, htmlBody, plainTextBody,
                                senderDisplay + " (" + tenantName + ")");
                        emailStatus = "EMAIL_SENT";
                        oauthSuccess = true;
                        emailConnectionService.markLastSend(conn.getId());
                        log.info("📧 Invitation email sent via Gmail OAuth from [{}] to [{}], msgId=[{}]",
                                conn.getEmailAddress(), cleanEmail, msgId);
                    }
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                log.warn("Gmail OAuth send attempt failed, falling back to central SMTP: {}", e.getMessage());
            }
        }

        // 2. Fallback: Dispatch via JavaMailSender (central SMTP / notifications sender)
        boolean smtpSuccess = false;
        if (!oauthSuccess) {
            try {
                if (mailSender != null) {
                    if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl jmsImpl) {
                        String centralUser = getCentralMailUsername();
                        String centralPass = getCentralMailPassword();
                        String envHost = System.getenv("SPRING_MAIL_HOST");
                        if (envHost == null || envHost.isBlank()) {
                            envHost = System.getProperty("SPRING_MAIL_HOST");
                        }
                        if (envHost != null && !envHost.isBlank()) {
                            jmsImpl.setHost(envHost.trim());
                        } else if (jmsImpl.getHost() == null || jmsImpl.getHost().isBlank()) {
                            jmsImpl.setHost(mailHost);
                        }

                        String envPort = System.getenv("SPRING_MAIL_PORT");
                        if (envPort == null || envPort.isBlank()) {
                            envPort = System.getProperty("SPRING_MAIL_PORT");
                        }
                        if (envPort != null && !envPort.isBlank()) {
                            try { jmsImpl.setPort(Integer.parseInt(envPort.trim())); } catch (Exception ignored) {}
                        } else if (jmsImpl.getPort() <= 0) {
                            jmsImpl.setPort(mailPort);
                        }
                        if (centralUser != null && !centralUser.isBlank()) {
                            jmsImpl.setUsername(centralUser);
                        } else if ((jmsImpl.getUsername() == null || jmsImpl.getUsername().isBlank())
                                && effectiveFrom != null && !effectiveFrom.contains("noreply")) {
                            jmsImpl.setUsername(effectiveFrom);
                        }
                        if (centralPass != null && !centralPass.isBlank()) {
                            jmsImpl.setPassword(centralPass);
                        }

                        java.util.Properties props = jmsImpl.getJavaMailProperties();
                        if (props == null) {
                            props = new java.util.Properties();
                            jmsImpl.setJavaMailProperties(props);
                        }
                        String envAuth = System.getenv("SPRING_MAIL_AUTH");
                        if (envAuth == null || envAuth.isBlank()) {
                            envAuth = System.getProperty("SPRING_MAIL_AUTH");
                        }
                        if (envAuth == null || envAuth.isBlank()) {
                            envAuth = System.getProperty("spring.mail.properties.mail.smtp.auth", "true");
                        }
                        props.put("mail.smtp.auth", envAuth.trim());

                        String envStarttls = System.getenv("SPRING_MAIL_STARTTLS_ENABLE");
                        if (envStarttls == null || envStarttls.isBlank()) {
                            envStarttls = System.getProperty("SPRING_MAIL_STARTTLS_ENABLE");
                        }
                        if (envStarttls == null || envStarttls.isBlank()) {
                            envStarttls = System.getProperty("spring.mail.properties.mail.smtp.starttls.enable", "true");
                        }
                        props.put("mail.smtp.starttls.enable", envStarttls.trim());

                        String envStarttlsReq = System.getenv("SPRING_MAIL_STARTTLS_REQUIRED");
                        if (envStarttlsReq == null || envStarttlsReq.isBlank()) {
                            envStarttlsReq = System.getProperty("SPRING_MAIL_STARTTLS_REQUIRED");
                        }
                        if (envStarttlsReq == null || envStarttlsReq.isBlank()) {
                            envStarttlsReq = System.getProperty("spring.mail.properties.mail.smtp.starttls.required", "true");
                        }
                        props.put("mail.smtp.starttls.required", envStarttlsReq.trim());

                        String envSslTrust = System.getenv("SPRING_MAIL_SSL_TRUST");
                        if (envSslTrust == null || envSslTrust.isBlank()) {
                            envSslTrust = System.getProperty("SPRING_MAIL_SSL_TRUST");
                        }
                        if (envSslTrust == null || envSslTrust.isBlank()) {
                            envSslTrust = System.getProperty("spring.mail.properties.mail.smtp.ssl.trust", "smtp.gmail.com");
                        }
                        props.put("mail.smtp.ssl.trust", envSslTrust.trim());
                        props.put("mail.smtp.connectiontimeout", "10000");
                        props.put("mail.smtp.timeout", "10000");
                        props.put("mail.smtp.writetimeout", "10000");
                    }

                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                    helper.setTo(cleanEmail);
                    if (effectiveFrom != null && !effectiveFrom.isBlank()) {
                        helper.setFrom(effectiveFrom, senderDisplay + " (" + tenantName + ")");
                    } else {
                        helper.setFrom("noreply@workhive.internal", tenantName + " via WorkHive");
                    }
                    if (senderEmail != null && !senderEmail.isBlank()) {
                        helper.setReplyTo(senderEmail);
                    }
                    helper.setSubject(subject);
                    helper.setText(plainTextBody, htmlBody);

                    mailSender.send(mimeMessage);
                    String effectiveHost = (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl jmsImpl && jmsImpl.getHost() != null && !jmsImpl.getHost().isBlank())
                            ? jmsImpl.getHost()
                            : getCentralMailHost();

                    if ("localhost".equalsIgnoreCase(effectiveHost) || "127.0.0.1".equals(effectiveHost)) {
                        log.warn("⚠️ Invitation email captured by local MailHog on {}:{} for [{}]. NOT delivered to real inbox.", effectiveHost, mailPort, cleanEmail);
                        emailStatus = "EMAIL_FAILED";
                        smtpSuccess = false;
                    } else {
                        emailStatus = "EMAIL_SENT";
                        smtpSuccess = true;
                        log.info("📧 Invitation email dispatched via real SMTP [{}:{}] from [{}] to recipient: [{}] (Reply-To: [{}])",
                                effectiveHost, mailPort, effectiveFrom, cleanEmail, senderEmail);
                    }
                } else {
                    log.warn("JavaMailSender bean not available. Invitation link: {}", inviteUrl);
                    emailStatus = "EMAIL_PENDING";
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
                log.error("❌ SMTP transmission error to recipient [{}]: {}", cleanEmail, e.getMessage());
                emailStatus = "EMAIL_FAILED";
                smtpSuccess = false;
            }
        }

        // 3. If neither OAuth nor SMTP dispatched, record in local MailHog buffer for dev tracing
        if (!oauthSuccess && !smtpSuccess) {
            try {
                MailHogServer.recordMessage(MailHogServer.MailMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .from(effectiveFrom)
                        .to(List.of(cleanEmail))
                        .subject(subject)
                        .bodyText(plainTextBody)
                        .bodyHtml(htmlBody)
                        .rawContent(plainTextBody)
                        .timestamp(Instant.now())
                        .headers(Map.of(
                                "to", cleanEmail,
                                "from", effectiveFrom,
                                "subject", subject,
                                "x-invitation-id", invitationId != null ? invitationId.toString() : "",
                                "x-tenant-id", tenantId.toString(),
                                "x-sender-admin", senderEmail != null ? senderEmail : "",
                                "x-error", errorMessage != null ? errorMessage : ""
                        ))
                        .build());
            } catch (Exception e) {
                log.debug("MailHog local recording note: {}", e.getMessage());
            }
        }

        // 3. Record Outbox Event for transaction audit & background processing
        try {
            String eventType = "EMAIL_SENT".equals(emailStatus) ? "EMAIL_INVITATION_SENT" : "EMAIL_INVITATION_FAILED";
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .tenantId(tenantId)
                    .eventType(eventType)
                    .aggregateType("INVITATION")
                    .aggregateId(invitationId)
                    .payload(String.format(
                            "{\"recipient\":\"%s\",\"employeeName\":\"%s\",\"tenantName\":\"%s\",\"role\":\"%s\",\"department\":\"%s\",\"team\":\"%s\",\"inviteUrl\":\"%s\",\"emailStatus\":\"%s\",\"senderEmail\":\"%s\",\"senderName\":\"%s\",\"error\":\"%s\"}",
                            cleanEmail,
                            escapeJson(displayName),
                            escapeJson(tenantName),
                            escapeJson(role),
                            escapeJson(departmentName != null ? departmentName : ""),
                            escapeJson(teamName != null ? teamName : ""),
                            inviteUrl,
                            emailStatus,
                            escapeJson(effectiveFrom != null ? effectiveFrom : ""),
                            escapeJson(senderDisplay != null ? senderDisplay : ""),
                            escapeJson(errorMessage != null ? errorMessage : "")
                    ))
                    .status("EMAIL_SENT".equals(emailStatus) ? "PROCESSED" : "PENDING")
                    .build();
            outboxEventRepository.save(outboxEvent);
        } catch (Exception e) {
            log.warn("Could not record invitation outbox event: {}", e.getMessage());
        }

        return emailStatus;
    }

    private String buildInvitationHtml(String tenantName, String displayName, String role,
                                       String departmentName, String teamName, String inviteUrl, String formattedExpiry,
                                       String senderEmail, String senderName) {
        String senderDisplay = (senderName != null && !senderName.isBlank()) ? escapeHtml(senderName) : "Workspace Administrator";
        String senderEmailDisplay = (senderEmail != null && !senderEmail.isBlank()) ? escapeHtml(senderEmail) : "";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<title>WorkHive Workspace Invitation</title>\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; background-color: #090d16; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #e2e8f0;\">\n" +
                "  <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #090d16; padding: 40px 10px;\">\n" +
                "    <tr>\n" +
                "      <td align=\"center\">\n" +
                "        <table role=\"presentation\" width=\"600\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width: 600px; width: 100%; background-color: #0f172a; border-radius: 20px; border: 1px solid #1e293b; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);\">\n" +
                "          <!-- Header -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 36px 40px 24px; background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 100%); border-bottom: 1px solid #1e293b;\">\n" +
                "              <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">\n" +
                "                <tr>\n" +
                "                  <td>\n" +
                "                    <div style=\"display: inline-block; background: linear-gradient(135deg, #6366f1, #8b5cf6); border-radius: 12px; width: 44px; height: 44px; text-align: center; line-height: 44px; color: #ffffff; font-weight: 900; font-size: 22px;\">W</div>\n" +
                "                    <span style=\"font-size: 22px; font-weight: 800; color: #ffffff; margin-left: 12px; vertical-align: middle; letter-spacing: -0.5px;\">WorkHive</span>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Content -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 40px;\">\n" +
                "              <h1 style=\"margin: 0 0 16px; font-size: 24px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px;\">You're invited to join " + escapeHtml(tenantName) + "!</h1>\n" +
                "              <p style=\"margin: 0 0 24px; font-size: 15px; line-height: 24px; color: #94a3b8;\">\n" +
                "                Hello <strong style=\"color: #f1f5f9;\">" + escapeHtml(displayName) + "</strong>,\n" +
                "              </p>\n" +
                "              <p style=\"margin: 0 0 28px; font-size: 15px; line-height: 24px; color: #cbd5e1;\">\n" +
                "                <strong style=\"color: #ffffff;\">" + senderDisplay + "</strong>" + (!senderEmailDisplay.isEmpty() ? " (<span style=\"color: #818cf8;\">" + senderEmailDisplay + "</span>)" : "") + " has invited you to join the official <strong style=\"color: #ffffff;\">" + escapeHtml(tenantName) + "</strong> workspace on WorkHive.\n" +
                "              </p>\n" +
                "              <!-- Org Details Box -->\n" +
                "              <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #1e293b; border-radius: 14px; border: 1px solid #334155; margin-bottom: 32px;\">\n" +
                "                <tr>\n" +
                "                  <td style=\"padding: 20px;\">\n" +
                "                    <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"6\">\n" +
                "                      <tr>\n" +
                "                        <td width=\"120\" style=\"font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;\">Workspace</td>\n" +
                "                        <td style=\"font-size: 14px; font-weight: 700; color: #ffffff;\">" + escapeHtml(tenantName) + "</td>\n" +
                "                      </tr>\n" +
                "                      <tr>\n" +
                "                        <td style=\"font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;\">Role</td>\n" +
                "                        <td style=\"font-size: 14px; font-weight: 600; color: #818cf8;\">" + escapeHtml(role != null ? role : "EMPLOYEE") + "</td>\n" +
                "                      </tr>\n" +
                (departmentName != null ? "                      <tr>\n                        <td style=\"font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;\">Department</td>\n                        <td style=\"font-size: 14px; font-weight: 600; color: #f1f5f9;\">" + escapeHtml(departmentName) + "</td>\n                      </tr>\n" : "") +
                (teamName != null ? "                      <tr>\n                        <td style=\"font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;\">Team</td>\n                        <td style=\"font-size: 14px; font-weight: 600; color: #f1f5f9;\">" + escapeHtml(teamName) + "</td>\n                      </tr>\n" : "") +
                "                      <tr>\n" +
                "                        <td style=\"font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;\">Invited By</td>\n" +
                "                        <td style=\"font-size: 14px; font-weight: 600; color: #f1f5f9;\">" + senderDisplay + (!senderEmailDisplay.isEmpty() ? " (" + senderEmailDisplay + ")" : "") + "</td>\n" +
                "                      </tr>\n" +
                "                    </table>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "              <!-- CTA Button -->\n" +
                "              <table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"margin-bottom: 28px;\">\n" +
                "                <tr>\n" +
                "                  <td align=\"center\">\n" +
                "                    <a href=\"" + inviteUrl + "\" target=\"_blank\" style=\"display: inline-block; background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%); color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; padding: 16px 36px; border-radius: 12px; box-shadow: 0 10px 20px -5px rgba(99, 102, 241, 0.4);\">Accept Invitation & Set Up Password &rarr;</a>\n" +
                "                  </td>\n" +
                "                </tr>\n" +
                "              </table>\n" +
                "              <!-- Link Fallback -->\n" +
                "              <p style=\"margin: 0 0 12px; font-size: 13px; line-height: 20px; color: #64748b;\">\n" +
                "                If the button above does not work, copy and paste this link into your browser:\n" +
                "              </p>\n" +
                "              <p style=\"margin: 0 0 28px; font-size: 12px; line-height: 18px; word-break: break-all;\">\n" +
                "                <a href=\"" + inviteUrl + "\" style=\"color: #818cf8; text-decoration: underline;\">" + inviteUrl + "</a>\n" +
                "              </p>\n" +
                "              <!-- Expiry Note -->\n" +
                "              <div style=\"padding: 14px 18px; background-color: rgba(245, 158, 11, 0.1); border: 1px solid rgba(245, 158, 11, 0.2); border-radius: 10px; font-size: 13px; color: #fbbf24; margin-bottom: 20px;\">\n" +
                "                ⏰ <strong>Expires:</strong> " + escapeHtml(formattedExpiry) + "\n" +
                "              </div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <!-- Footer -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 24px 40px; background-color: #090d16; border-top: 1px solid #1e293b; font-size: 12px; color: #64748b; text-align: center;\">\n" +
                "              <p style=\"margin: 0 0 6px;\">&copy; 2026 WorkHive SaaS Inc. All rights reserved.</p>\n" +
                "              <p style=\"margin: 0;\">This invitation was authorized and dispatched by " + senderDisplay + " (" + senderEmailDisplay + ") on behalf of " + escapeHtml(tenantName) + ".</p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * Resolve the admin user ID from tenantId + senderEmail.
     * If emailConnectionService can find a connection for the inviting admin, return userId from TenantContext.
     * This is a lightweight lookup — the InvitationService already passes the admin's email.
     */
    private UUID findAdminUserId(UUID tenantId, String senderEmail) {
        // The inviting admin is the current authenticated user in TenantContext
        try {
            return com.workhive.security.TenantContext.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    public String getCentralMailUsername() {
        if (fromAddress != null && !fromAddress.isBlank() && !fromAddress.contains("noreply")) {
            return fromAddress.trim();
        }
        String envUser = System.getenv("SPRING_MAIL_USERNAME");
        if (envUser != null && !envUser.isBlank()) {
            return envUser.trim();
        }
        String sysUser = System.getProperty("SPRING_MAIL_USERNAME");
        if (sysUser != null && !sysUser.isBlank()) {
            return sysUser.trim();
        }
        String propUser = System.getProperty("spring.mail.username");
        if (propUser != null && !propUser.isBlank()) {
            return propUser.trim();
        }
        return null;
    }

    public String getCentralMailPassword() {
        String envPass = System.getenv("SPRING_MAIL_PASSWORD");
        if (envPass != null && !envPass.isBlank()) {
            return envPass.trim().replace(" ", "");
        }
        String sysPass = System.getProperty("SPRING_MAIL_PASSWORD");
        if (sysPass != null && !sysPass.isBlank()) {
            return sysPass.trim().replace(" ", "");
        }
        String propPass = System.getProperty("spring.mail.password");
        if (propPass != null && !propPass.isBlank()) {
            return propPass.trim().replace(" ", "");
        }
        return null;
    }

    public String getCentralMailHost() {
        String envHost = System.getenv("SPRING_MAIL_HOST");
        if (envHost != null && !envHost.isBlank()) {
            return envHost.trim();
        }
        String sysHost = System.getProperty("SPRING_MAIL_HOST");
        if (sysHost != null && !sysHost.isBlank()) {
            return sysHost.trim();
        }
        String propHost = System.getProperty("spring.mail.host");
        if (propHost != null && !propHost.isBlank()) {
            return propHost.trim();
        }
        return (mailHost != null && !mailHost.isBlank()) ? mailHost : "smtp.gmail.com";
    }

    public int getCentralMailPort() {
        String envPort = System.getenv("SPRING_MAIL_PORT");
        if (envPort != null && !envPort.isBlank()) {
            try { return Integer.parseInt(envPort.trim()); } catch (Exception ignored) {}
        }
        String sysPort = System.getProperty("SPRING_MAIL_PORT");
        if (sysPort != null && !sysPort.isBlank()) {
            try { return Integer.parseInt(sysPort.trim()); } catch (Exception ignored) {}
        }
        return mailPort > 0 ? mailPort : 587;
    }
}
