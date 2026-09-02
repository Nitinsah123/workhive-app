package com.workhive.module.auth.controller;

import com.workhive.module.auth.dto.AuthDtos.*;
import com.workhive.module.auth.service.AuthService;
import com.workhive.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = {"/workspace", "/create-workspace"})
    public ResponseEntity<AuthResponse> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        AuthResponse response = authService.createWorkspace(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        authService.logout(TenantContext.requireUserId());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/clean-test-users")
    public ResponseEntity<Map<String, Object>> cleanTestUsers() {
        Map<String, Object> result = authService.cleanTestAccounts();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        // This endpoint requires authentication (not in auth filter exclusions above /api/auth/)
        // But we include it here for convenience â€” the JWT filter will be updated
        return ResponseEntity.ok(Map.of(
            "userId", TenantContext.requireUserId().toString(),
            "tenantId", TenantContext.requireTenantId().toString(),
            "role", TenantContext.getRole()
        ));
    }
}
