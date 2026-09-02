package com.workhive.module.user.service.email;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhive.common.util.CryptoUtils;
import com.workhive.module.user.entity.EmailConnection;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;

/**
 * Gmail OAuth2 provider implementation.
 * Uses Google OAuth2 authorization code flow and Gmail API for sending emails.
 */
@Component
public class GmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(GmailProvider.class);

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";
    private static final String SCOPES = "https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/userinfo.email";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.oauth.client-id:}")
    private String clientId;

    @Value("${google.oauth.client-secret:}")
    private String clientSecret;

    public GmailProvider(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public String getEffectiveClientId() {
        if (clientId != null && !clientId.isBlank()) return clientId.trim();
        String env = System.getenv("GOOGLE_OAUTH_CLIENT_ID");
        if (env != null && !env.isBlank()) return env.trim();
        return System.getProperty("GOOGLE_OAUTH_CLIENT_ID", "").trim();
    }

    public String getEffectiveClientSecret() {
        if (clientSecret != null && !clientSecret.isBlank()) return clientSecret.trim();
        String env = System.getenv("GOOGLE_OAUTH_CLIENT_SECRET");
        if (env != null && !env.isBlank()) return env.trim();
        return System.getProperty("GOOGLE_OAUTH_CLIENT_SECRET", "").trim();
    }

    @Override
    public boolean isConfigured() {
        String id = getEffectiveClientId();
        String secret = getEffectiveClientSecret();
        return !id.isBlank() && !secret.isBlank();
    }

    @Override
    public String getProviderName() {
        return "GMAIL";
    }

    @Override
    public boolean canSend(EmailConnection connection) {
        return connection != null
                && "CONNECTED".equals(connection.getStatus())
                && connection.getAccessTokenEnc() != null
                && connection.getRefreshTokenEnc() != null;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.workhive.module.user.repository.EmailConnectionRepository emailConnectionRepository;

    @Override
    public String sendEmail(EmailConnection connection, String toEmail, String subject,
                            String htmlBody, String textBody, String fromDisplayName) throws Exception {

        // Check if token is expired or expiring in < 60s and refresh if needed
        if (connection.getTokenExpiresAt() != null && Instant.now().plusSeconds(60).isAfter(connection.getTokenExpiresAt())) {
            log.info("Gmail OAuth token expired or expiring soon for [{}], refreshing...", connection.getEmailAddress());
            connection = refreshAccessToken(connection);
        }

        String accessToken = CryptoUtils.decrypt(connection.getAccessTokenEnc());

        // Build raw MIME message
        String rawMessage = buildRawMimeMessage(
                connection.getEmailAddress(), fromDisplayName,
                toEmail, subject, htmlBody, textBody
        );

        String requestBody = "{\"raw\":\"" + rawMessage + "\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(GMAIL_SEND_URL, HttpMethod.POST, entity, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized unauthorizedEx) {
            log.warn("Gmail API returned 401 Unauthorized for [{}]. Attempting token refresh & retry...", connection.getEmailAddress());
            connection = refreshAccessToken(connection);
            accessToken = CryptoUtils.decrypt(connection.getAccessTokenEnc());

            headers.setBearerAuth(accessToken);
            HttpEntity<String> retryEntity = new HttpEntity<>(requestBody, headers);
            response = restTemplate.exchange(GMAIL_SEND_URL, HttpMethod.POST, retryEntity, String.class);
        }

        if (response.getStatusCode().is2xxSuccessful()) {
            JsonNode responseBody = objectMapper.readTree(response.getBody());
            String messageId = responseBody.has("id") ? responseBody.get("id").asText() : "unknown";
            log.info("📧 Gmail API: Email sent successfully. MessageId=[{}], From=[{}], To=[{}]",
                    messageId, connection.getEmailAddress(), toEmail);
            return messageId;
        } else {
            throw new RuntimeException("Gmail API returned status: " + response.getStatusCode());
        }
    }

    @Override
    public String getAuthorizationUrl(String state, String redirectUri) {
        String effectiveId = getEffectiveClientId();
        return GOOGLE_AUTH_URL
                + "?client_id=" + urlEncode(effectiveId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&response_type=code"
                + "&scope=" + urlEncode(SCOPES)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + urlEncode(state);
    }

    @Override
    public EmailConnection exchangeAuthorizationCode(String code, String redirectUri, EmailConnection connection) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", getEffectiveClientId());
        params.add("client_secret", getEffectiveClientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(GOOGLE_TOKEN_URL, HttpMethod.POST, entity, String.class);
            JsonNode tokenData = objectMapper.readTree(response.getBody());

            String accessToken = tokenData.get("access_token").asText();
            String refreshToken = tokenData.has("refresh_token") ? tokenData.get("refresh_token").asText() : null;
            int expiresIn = tokenData.has("expires_in") ? tokenData.get("expires_in").asInt() : 3600;

            // Encrypt tokens
            connection.setAccessTokenEnc(CryptoUtils.encrypt(accessToken));
            if (refreshToken != null) {
                connection.setRefreshTokenEnc(CryptoUtils.encrypt(refreshToken));
            }
            connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            connection.setScopes(SCOPES);

            // Fetch the user's email address from Google (try id_token first if present, then userinfo endpoint)
            String emailAddress = null;
            if (tokenData.has("id_token")) {
                try {
                    String idToken = tokenData.get("id_token").asText();
                    String[] parts = idToken.split("\\.");
                    if (parts.length >= 2) {
                        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                        JsonNode payloadJson = objectMapper.readTree(payload);
                        if (payloadJson.has("email")) {
                            emailAddress = payloadJson.get("email").asText();
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not parse email from id_token: {}", e.getMessage());
                }
            }
            if (emailAddress == null || emailAddress.isBlank()) {
                emailAddress = fetchUserEmail(accessToken);
            }

            connection.setEmailAddress(emailAddress);
            connection.setStatus("CONNECTED");
            connection.setErrorMessage(null);
            connection.setOauthState(null); // Clear state after successful exchange

            log.info("✅ Gmail OAuth connected successfully for email: [{}]", emailAddress);
            return connection;

        } catch (Exception e) {
            log.error("❌ Gmail OAuth code exchange failed: {}", e.getMessage());
            connection.setStatus("ERROR");
            connection.setErrorMessage("OAuth code exchange failed: " + e.getMessage());
            return connection;
        }
    }

    @Override
    public EmailConnection refreshAccessToken(EmailConnection connection) {
        String refreshToken = CryptoUtils.decrypt(connection.getRefreshTokenEnc());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", getEffectiveClientId());
        params.add("client_secret", getEffectiveClientSecret());
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(GOOGLE_TOKEN_URL, HttpMethod.POST, entity, String.class);
            JsonNode tokenData = objectMapper.readTree(response.getBody());

            String newAccessToken = tokenData.get("access_token").asText();
            int expiresIn = tokenData.has("expires_in") ? tokenData.get("expires_in").asInt() : 3600;

            connection.setAccessTokenEnc(CryptoUtils.encrypt(newAccessToken));
            connection.setTokenExpiresAt(Instant.now().plusSeconds(expiresIn));
            connection.setStatus("CONNECTED");
            connection.setErrorMessage(null);

            if (emailConnectionRepository != null && connection.getId() != null) {
                try {
                    emailConnectionRepository.save(connection);
                } catch (Exception e) {
                    log.warn("Could not immediately persist refreshed Gmail connection: {}", e.getMessage());
                }
            }

            log.info("🔄 Gmail OAuth token refreshed for [{}]", connection.getEmailAddress());
            return connection;

        } catch (Exception e) {
            log.error("❌ Gmail OAuth token refresh failed for [{}]: {}", connection.getEmailAddress(), e.getMessage());
            connection.setStatus("REAUTH_REQUIRED");
            connection.setErrorMessage("Token refresh failed: " + e.getMessage());
            if (emailConnectionRepository != null && connection.getId() != null) {
                try {
                    emailConnectionRepository.save(connection);
                } catch (Exception ignored) {}
            }
            return connection;
        }
    }

    @Override
    public void revokeAccess(EmailConnection connection) {
        try {
            String accessToken = CryptoUtils.decrypt(connection.getAccessTokenEnc());
            if (accessToken != null && !accessToken.isBlank()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("token", accessToken);

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);
                restTemplate.exchange(GOOGLE_REVOKE_URL, HttpMethod.POST, entity, String.class);
                log.info("🔓 Gmail OAuth access revoked for [{}]", connection.getEmailAddress());
            }
        } catch (Exception e) {
            log.warn("Gmail OAuth revocation note: {}", e.getMessage());
        }
    }

    // ─── Private helpers ───────────────────────────────────────────

    private String fetchUserEmail(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(GOOGLE_USERINFO_URL, HttpMethod.GET, entity, String.class);
        JsonNode userInfo = objectMapper.readTree(response.getBody());

        return userInfo.has("email") ? userInfo.get("email").asText() : null;
    }

    private String buildRawMimeMessage(String from, String fromDisplay, String to,
                                       String subject, String htmlBody, String textBody) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage mimeMessage = new MimeMessage(session);
        if (fromDisplay != null && !fromDisplay.isBlank()) {
            mimeMessage.setFrom(new InternetAddress(from, fromDisplay, "UTF-8"));
        } else {
            mimeMessage.setFrom(new InternetAddress(from));
        }
        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        mimeMessage.setSubject(subject, "UTF-8");

        // Multipart alternative: text + html
        jakarta.mail.Multipart multipart = new jakarta.mail.internet.MimeMultipart("alternative");

        jakarta.mail.internet.MimeBodyPart textPart = new jakarta.mail.internet.MimeBodyPart();
        textPart.setText(textBody, "UTF-8");
        multipart.addBodyPart(textPart);

        jakarta.mail.internet.MimeBodyPart htmlPart = new jakarta.mail.internet.MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        mimeMessage.setContent(multipart);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);

        // Gmail API requires URL-safe Base64
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
