package com.workhive.security;

import java.util.UUID;

/**
 * Thread-local tenant context. Set from JWT claims during authentication.
 * NEVER set from client-supplied data (URL, body, query params).
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();

    private TenantContext() {}

    public static void setContext(UUID userId, UUID tenantId, String role) {
        CURRENT_USER.set(userId);
        CURRENT_TENANT.set(tenantId);
        CURRENT_ROLE.set(role);
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static UUID requireTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new SecurityException("No tenant context available");
        }
        return tenantId;
    }

    public static void setUserId(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT_USER.get();
    }

    public static UUID requireUserId() {
        UUID userId = CURRENT_USER.get();
        if (userId == null) {
            throw new SecurityException("No user context available");
        }
        return userId;
    }

    public static void setRole(String role) {
        CURRENT_ROLE.set(role);
    }

    public static String getRole() {
        return CURRENT_ROLE.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
        CURRENT_ROLE.remove();
    }
}
