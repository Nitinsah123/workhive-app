package com.workhive.security;

import java.util.UUID;

/**
 * Holds authenticated user details extracted from JWT.
 */
public record AuthUserDetails(
    UUID userId,
    UUID tenantId,
    String email,
    String role
) {}
