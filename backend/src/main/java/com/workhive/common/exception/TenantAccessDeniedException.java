package com.workhive.common.exception;

public class TenantAccessDeniedException extends RuntimeException {
    public TenantAccessDeniedException(String message) { super(message); }
    public TenantAccessDeniedException() { super("Cross-tenant access denied"); }
}
