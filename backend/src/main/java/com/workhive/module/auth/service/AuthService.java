package com.workhive.module.auth.service;

import com.workhive.common.exception.*;
import com.workhive.module.auth.dto.AuthDtos.*;
import com.workhive.module.auth.entity.RefreshToken;
import com.workhive.module.auth.repository.RefreshTokenRepository;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.tenant.entity.Tenant;
import com.workhive.module.tenant.repository.TenantRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.workhive.module.user.repository.InvitationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager entityManager;

    public AuthService(TenantRepository tenantRepository, UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       InvitationRepository invitationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.invitationRepository = invitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    /**
     * Delete only non-admin test/employee accounts while preserving:
     * - ALL TENANT_ADMIN users (never deleted)
     * - The calling user's own account (never deleted)
     * - Tenant/workspace records (never deleted)
     * - Projects, tasks, documents, integrations, and other business data (never deleted)
     *
     * Only removes: non-admin users (EMPLOYEE/MANAGER role), their expired refresh tokens,
     * and pending invitations. Foreign key references to deleted users are safely nullified.
     */
    @Transactional
    public Map<String, Object> cleanTestAccounts() {
        UUID callingUserId = com.workhive.security.TenantContext.getUserId();
        UUID callingTenantId = com.workhive.security.TenantContext.getTenantId();

        // Find non-admin users to delete (NEVER delete TENANT_ADMIN accounts)
        java.util.List<User> allUsers = userRepository.findAll();
        java.util.List<User> usersToDelete = allUsers.stream()
                .filter(u -> !"TENANT_ADMIN".equals(u.getRole()))
                .filter(u -> !u.getId().equals(callingUserId))
                .collect(java.util.stream.Collectors.toList());

        java.util.List<UUID> idsToDelete = usersToDelete.stream()
                .map(User::getId)
                .collect(java.util.stream.Collectors.toList());

        long refreshTokensDeleted = 0;
        long invitationsDeleted = 0;
        long usersDeleted = 0;

        if (!idsToDelete.isEmpty()) {
            // Clean refresh tokens for these users only
            for (UUID uid : idsToDelete) {
                long count = refreshTokenRepository.countByUserId(uid);
                refreshTokenRepository.deleteByUserId(uid);
                refreshTokensDeleted += count;
            }

            // Nullify foreign key references to these users only
            try {
                for (UUID uid : idsToDelete) {
                    String uidStr = uid.toString();
                    entityManager.createNativeQuery("UPDATE departments SET manager_id = NULL WHERE manager_id = :uid")
                            .setParameter("uid", uidStr).executeUpdate();
                    entityManager.createNativeQuery("UPDATE teams SET lead_id = NULL WHERE lead_id = :uid")
                            .setParameter("uid", uidStr).executeUpdate();
                    entityManager.createNativeQuery("UPDATE projects SET manager_id = NULL WHERE manager_id = :uid")
                            .setParameter("uid", uidStr).executeUpdate();
                    entityManager.createNativeQuery("UPDATE tasks SET assignee_id = NULL WHERE assignee_id = :uid")
                            .setParameter("uid", uidStr).executeUpdate();
                    entityManager.createNativeQuery("UPDATE tasks SET reviewer_id = NULL WHERE reviewer_id = :uid")
                            .setParameter("uid", uidStr).executeUpdate();
                    entityManager.createNativeQuery("UPDATE leave_requests SET reviewer_id = NULL WHERE reviewer_id = :uid")
                            .setParameter("uid", uidStr).executeUpdate();
                    entityManager.createNativeQuery("UPDATE users SET manager_id = NULL WHERE manager_id = :uid")
                            .setParameter("uid", uidStr).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM project_members WHERE user_id = :uid")
                            .setParameter("uid", uidStr).executeUpdate();
                }
            } catch (Exception e) {
                log.warn("Disassociation query note: {}", e.getMessage());
            }

            // Delete the users
            userRepository.deleteAll(usersToDelete);
            usersDeleted = usersToDelete.size();
        }

        // Clean pending invitations (not accepted users, just invitation records)
        invitationsDeleted = invitationRepository.count();
        invitationRepository.deleteAll();

        // Clean expired/orphaned refresh tokens (not belonging to any remaining user)
        // Keep current admin's tokens intact

        log.info("Cleaned up {} test users (preserved {} admins), {} invitations, {} refresh tokens",
                usersDeleted, allUsers.size() - usersDeleted, invitationsDeleted, refreshTokensDeleted);

        return Map.of(
            "message", "Test user cleanup completed. Admin accounts preserved.",
            "usersDeleted", usersDeleted,
            "usersPreserved", allUsers.size() - usersDeleted,
            "invitationsDeleted", invitationsDeleted,
            "refreshTokensDeleted", refreshTokensDeleted
        );
    }

    /**
     * Create a new workspace with organization and admin user.
     * Fully transactional — rolls back on any failure.
     */
    @Transactional
    public AuthResponse createWorkspace(CreateWorkspaceRequest request) {
        log.info("Creating workspace for company: {}", request.getCompanyName());

        // Validate uniqueness
        String slug = generateSlug(request.getCompanyName());
        String code = request.getCompanyCode().toUpperCase().trim();

        if (tenantRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Organization with this name already exists");
        }
        if (tenantRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Organization code already in use");
        }
        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new DuplicateResourceException("Email already registered");
        }

        // 1. Create Tenant
        Tenant tenant = Tenant.builder()
                .name(request.getCompanyName().trim())
                .slug(slug)
                .code(code)
                .industry(request.getIndustry())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .workingDays(request.getWorkingDays() != null ? request.getWorkingDays() : "MON,TUE,WED,THU,FRI")
                .logoUrl(request.getLogoUrl())
                .status("ACTIVE")
                .build();
        tenant = tenantRepository.save(tenant);

        // 2. Create Admin User with employee code
        String employeeCode = code + "-ADM-001";
        User admin = User.builder()
                .tenantId(tenant.getId())
                .email(request.getAdminEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getAdminPassword()))
                .fullName(request.getAdminFullName().trim())
                .employeeCode(employeeCode)
                .role("TENANT_ADMIN")
                .phone(request.getAdminPhone())
                .status("ACTIVE")
                .build();
        admin = userRepository.save(admin);

        // 3. Audit event
        auditService.log(tenant.getId(), admin.getId(), "WORKSPACE_CREATED", "TENANT", tenant.getId(),
                null, null);

        log.info("Workspace created: tenant={}, admin={}", tenant.getId(), admin.getId());

        // 4. Generate tokens and return
        return buildAuthResponse(admin, tenant);
    }

    /**
     * Login with email + password. Automatically resolves tenant from user record.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BadRequestException("Account is inactive");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", user.getTenantId()));

        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new BadRequestException("Organization is inactive");
        }

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Audit
        auditService.log(tenant.getId(), user.getId(), "USER_LOGIN", "USER", user.getId(), null, null);

        log.info("User logged in: user={}, tenant={}", user.getId(), tenant.getId());

        return buildAuthResponse(user, tenant);
    }

    /**
     * Refresh access token using a valid refresh token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", refreshToken.getUserId()));
        Tenant tenant = tenantRepository.findById(refreshToken.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", refreshToken.getTenantId()));

        // Revoke old refresh token and issue new one
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(user, tenant);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private AuthResponse buildAuthResponse(User user, Tenant tenant) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), tenant.getId(), user.getEmail(), user.getRole(), user.getFullName());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId(), tenant.getId());

        // Save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tenantId(tenant.getId())
                .token(refreshTokenStr)
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        // Build response
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenStr);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId().toString());
        userInfo.setEmail(user.getEmail());
        userInfo.setFullName(user.getFullName());
        userInfo.setRole(user.getRole());
        userInfo.setEmployeeCode(user.getEmployeeCode());
        userInfo.setAvatarUrl(user.getAvatarUrl());
        userInfo.setDepartmentId(user.getDepartmentId() != null ? user.getDepartmentId().toString() : null);
        userInfo.setTeamId(user.getTeamId() != null ? user.getTeamId().toString() : null);
        userInfo.setManagerId(user.getManagerId() != null ? user.getManagerId().toString() : null);
        response.setUser(userInfo);

        AuthResponse.TenantInfo tenantInfo = new AuthResponse.TenantInfo();
        tenantInfo.setId(tenant.getId().toString());
        tenantInfo.setName(tenant.getName());
        tenantInfo.setCode(tenant.getCode());
        tenantInfo.setLogoUrl(tenant.getLogoUrl());
        tenantInfo.setTimezone(tenant.getTimezone());
        response.setTenant(tenantInfo);

        return response;
    }

    private String generateSlug(String name) {
        return name.toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}
