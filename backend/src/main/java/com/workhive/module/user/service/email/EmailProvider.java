package com.workhive.module.user.service.email;

import com.workhive.module.user.entity.EmailConnection;

/**
 * Provider abstraction for sending emails via different OAuth-authorized services.
 * Implementations: GmailProvider, (future: OutlookProvider, etc.)
 */
public interface EmailProvider {

    /** Returns the provider identifier, e.g. "GMAIL", "OUTLOOK" */
    String getProviderName();

    /** Check if the provider is configured with credentials on the server */
    default boolean isConfigured() {
        return true;
    }

    /** Check if the connection is in a state that allows sending */
    boolean canSend(EmailConnection connection);

    /**
     * Send an email using the provider's API with OAuth credentials.
     *
     * @return message ID from the provider
     * @throws Exception if sending fails
     */
    String sendEmail(EmailConnection connection,
                     String toEmail,
                     String subject,
                     String htmlBody,
                     String textBody,
                     String fromDisplayName) throws Exception;

    /**
     * Generate the OAuth authorization URL for user consent.
     *
     * @param state CSRF state parameter
     * @param redirectUri OAuth callback URI
     * @return full authorization URL
     */
    String getAuthorizationUrl(String state, String redirectUri);

    /**
     * Exchange an authorization code for access/refresh tokens.
     * Updates the EmailConnection with encrypted tokens and email address.
     *
     * @return updated EmailConnection (not yet persisted)
     */
    EmailConnection exchangeAuthorizationCode(String code, String redirectUri, EmailConnection connection);

    /**
     * Refresh an expired access token using the stored refresh token.
     *
     * @return updated EmailConnection with new access token
     */
    EmailConnection refreshAccessToken(EmailConnection connection);

    /**
     * Revoke OAuth access and clean up tokens.
     */
    void revokeAccess(EmailConnection connection);
}
